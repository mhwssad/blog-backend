package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelAccount;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelAccountRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountVO;
import com.cybzacg.blogbackend.module.ai.service.AiChannelAccountAdminService;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.account.service.TwoFactorService;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 渠道账号池后台管理服务实现。
 *
 * <p>负责渠道账号的增删改查、状态变更、API Key 安全审计（变更需超级管理员 + MFA 验证）
 * 以及操作审计日志记录。所有返回的 VO 中 API Key 均已脱敏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChannelAccountAdminServiceImpl implements AiChannelAccountAdminService {

    private final AiChannelAccountRepository aiChannelAccountRepository;
    private final AiChannelConfigRepository aiChannelConfigRepository;
    private final AiModelConvert aiModelConvert;
    private final SysAuditLogService sysAuditLogService;
    private final TwoFactorService twoFactorService;
    private final SuperAdminVerifier superAdminVerifier;

    /**
     * 分页查询指定渠道下的账号列表，按权重降序、ID 升序排列，API Key 脱敏返回。
     *
     * @param channelConfigId 渠道配置 ID
     * @param current         当前页码
     * @param size            每页条数
     * @return 分页结果
     * @throws com.cybzacg.blogbackend.exception.BusinessException 渠道不存在时抛出
     */
    @Override
    public PageResult<AiChannelAccountVO> listAccounts(Long channelConfigId, long current, long size) {
        verifyChannelExists(channelConfigId);
        current = PaginationUtils.normalizeCurrent(current);
        size = PaginationUtils.normalizeSize(size, 10L, 100L);

        Page<AiChannelAccount> page = aiChannelAccountRepository.page(
                new Page<>(current, size),
                new LambdaQueryWrapper<AiChannelAccount>()
                        .eq(AiChannelAccount::getChannelConfigId, channelConfigId)
                        .orderByDesc(AiChannelAccount::getWeight)
                        .orderByAsc(AiChannelAccount::getId));
        List<AiChannelAccountVO> records = page.getRecords().stream()
                .map(this::toMaskedVO)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 获取指定渠道下的账号详情，API Key 脱敏返回。
     *
     * @param channelConfigId 渠道配置 ID
     * @param accountId       账号 ID
     * @return 账号 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 渠道不存在或账号不属于该渠道时抛出
     */
    @Override
    public AiChannelAccountVO getAccount(Long channelConfigId, Long accountId) {
        verifyChannelExists(channelConfigId);
        AiChannelAccount account = findAndVerifyOwnership(channelConfigId, accountId);
        return toMaskedVO(account);
    }

    /**
     * 在指定渠道下创建新账号，未指定的字段使用默认值。
     *
     * @param channelConfigId 渠道配置 ID
     * @param request         账号创建请求
     * @param operatorId      操作人 ID
     * @return 创建后的账号 VO（API Key 已脱敏）
     * @throws com.cybzacg.blogbackend.exception.BusinessException 渠道不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChannelAccountVO createAccount(Long channelConfigId, AiChannelAccountSaveRequest request, Long operatorId) {
        verifyChannelExists(channelConfigId);

        AiChannelAccount account = aiModelConvert.toChannelAccount(request);
        account.setChannelConfigId(channelConfigId);
        account.setCreatedBy(operatorId);
        account.setUpdatedBy(operatorId);
        if (account.getWeight() == null) {
            account.setWeight(1);
        }
        if (account.getStatus() == null) {
            account.setStatus(1);
        }
        if (account.getDailyQuota() == null) {
            account.setDailyQuota(0);
        }
        if (account.getMaxConsecutiveErrors() == null) {
            account.setMaxConsecutiveErrors(5);
        }
        aiChannelAccountRepository.save(account);

        log.info("创建 AI 渠道账号: channelConfigId={}, accountName={}, operatorId={}",
                channelConfigId, account.getAccountName(), operatorId);
        recordAuditLog(operatorId, channelConfigId, "创建账号: " + account.getAccountName());
        return toMaskedVO(account);
    }

    /**
     * 更新指定账号的配置信息。
     *
     * <p>API Key 安全机制：若请求中的 apiKey 包含掩码标记（****），则保留原值不变；
     * 若发生真实 API Key 变更，需通过超级管理员权限 + MFA 二次验证。
     *
     * @param channelConfigId 渠道配置 ID
     * @param accountId       账号 ID
     * @param request         更新请求
     * @param operatorId      操作人 ID
     * @return 更新后的账号 VO（API Key 已脱敏）
     * @throws com.cybzacg.blogbackend.exception.BusinessException 渠道不存在、账号不属于该渠道或 API Key 变更未通过 MFA 时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChannelAccountVO updateAccount(Long channelConfigId, Long accountId,
                                             AiChannelAccountSaveRequest request, Long operatorId) {
        verifyChannelExists(channelConfigId);
        AiChannelAccount account = findAndVerifyOwnership(channelConfigId, accountId);

        // API Key 掩码检测：包含 **** 则保留原始值
        if (request.getApiKeyEncrypted() != null && request.getApiKeyEncrypted().contains("****")) {
            request.setApiKeyEncrypted(account.getApiKeyEncrypted());
        }

        // 高风险审计：API Key 变更
        auditApiKeyChange(account, request, operatorId);

        aiModelConvert.updateChannelAccount(request, account);
        account.setUpdatedBy(operatorId);
        aiChannelAccountRepository.updateById(account);

        return toMaskedVO(account);
    }

    /**
     * 更新指定账号的状态（启用/禁用）。
     *
     * @param channelConfigId 渠道配置 ID
     * @param accountId       账号 ID
     * @param status          目标状态
     * @param operatorId      操作人 ID
     * @throws com.cybzacg.blogbackend.exception.BusinessException 渠道不存在或账号不属于该渠道时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccountStatus(Long channelConfigId, Long accountId, Integer status, Long operatorId) {
        verifyChannelExists(channelConfigId);
        AiChannelAccount account = findAndVerifyOwnership(channelConfigId, accountId);

        account.setStatus(status);
        account.setUpdatedBy(operatorId);
        aiChannelAccountRepository.updateById(account);

        recordAuditLog(operatorId, channelConfigId,
                "账号 [" + account.getAccountName() + "] 状态变更为 " + status);
    }

    /**
     * 删除指定渠道下的账号。
     *
     * @param channelConfigId 渠道配置 ID
     * @param accountId       账号 ID
     * @param operatorId      操作人 ID
     * @throws com.cybzacg.blogbackend.exception.BusinessException 渠道不存在或账号不属于该渠道时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long channelConfigId, Long accountId, Long operatorId) {
        verifyChannelExists(channelConfigId);
        AiChannelAccount account = findAndVerifyOwnership(channelConfigId, accountId);
        aiChannelAccountRepository.removeById(accountId);

        log.info("删除 AI 渠道账号: channelConfigId={}, accountId={}, accountName={}, operatorId={}",
                channelConfigId, accountId, account.getAccountName(), operatorId);
        recordAuditLog(operatorId, channelConfigId,
                "删除账号: " + account.getAccountName());
    }

    /**
     * 查找账号并验证其属于指定渠道。
     *
     * @throws com.cybzacg.blogbackend.exception.BusinessException 账号不存在或不属于该渠道时抛出
     */
    private AiChannelAccount findAndVerifyOwnership(Long channelConfigId, Long accountId) {
        AiChannelAccount account = ExceptionThrowerCore.requireNonNull(
                aiChannelAccountRepository.getById(accountId), ResultErrorCode.AI_ACCOUNT_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(
                !channelConfigId.equals(account.getChannelConfigId()),
                ResultErrorCode.AI_ACCOUNT_NOT_FOUND);
        return account;
    }

    /**
     * 校验渠道配置是否存在，不存在则抛出异常。
     */
    private void verifyChannelExists(Long channelConfigId) {
        AiChannelConfig config = aiChannelConfigRepository.getById(channelConfigId);
        ExceptionThrowerCore.throwBusinessIfNull(config, ResultErrorCode.AI_CHANNEL_NOT_FOUND);
    }

    /**
     * 将账号实体转换为 VO 并对 API Key 进行脱敏处理。
     */
    private AiChannelAccountVO toMaskedVO(AiChannelAccount account) {
        AiChannelAccountVO vo = aiModelConvert.toChannelAccountVO(account);
        if (vo != null && vo.getApiKeyEncrypted() != null && vo.getApiKeyEncrypted().length() > 7) {
            vo.setApiKeyEncrypted(AiChannelConfigAdminServiceImpl.maskApiKey(vo.getApiKeyEncrypted()));
        }
        return vo;
    }

    /**
     * 检测 API Key 是否发生真实变更，若变更则要求超级管理员权限和 MFA 验证。
     *
     * <p>这是高风险审计操作，防止 API Key 被未授权替换。
     */
    private void auditApiKeyChange(AiChannelAccount existing, AiChannelAccountSaveRequest request, Long operatorId) {
        // 判断 API Key 是否发生真实变更（排除掩码占位符和未修改的情况）
        boolean apiKeyChanged = request.getApiKeyEncrypted() != null
                && !request.getApiKeyEncrypted().contains("****")
                && !request.getApiKeyEncrypted().equals(existing.getApiKeyEncrypted());

        if (apiKeyChanged) {
            superAdminVerifier.requireSuperAdmin(operatorId);
            ExceptionThrowerCore.throwBusinessIfNot(
                    twoFactorService.validateTicket(request.getMfaTicket(), operatorId),
                    ResultErrorCode.MFA_TICKET_INVALID);

            recordAuditLog(operatorId, existing.getChannelConfigId(),
                    "账号 [" + existing.getAccountName() + "] API Key 变更");
        }
    }

    /**
     * 记录 AI 配置变更审计日志。
     */
    private void recordAuditLog(Long operatorId, Long channelConfigId, String afterState) {
        SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
        auditRequest.setOperatorUserId(operatorId);
        auditRequest.setOperationType(SysAuditOperationType.MODIFY_AI_CONFIG.getCode());
        auditRequest.setTargetTypeName("AiChannelAccount");
        auditRequest.setTargetId(channelConfigId);
        auditRequest.setAfterState(afterState);
        sysAuditLogService.record(auditRequest);
    }
}
