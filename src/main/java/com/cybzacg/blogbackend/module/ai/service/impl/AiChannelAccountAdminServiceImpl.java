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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 渠道账号池后台管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiChannelAccountAdminServiceImpl implements AiChannelAccountAdminService {

    private final AiChannelAccountRepository aiChannelAccountRepository;
    private final AiChannelConfigRepository aiChannelConfigRepository;
    private final AiModelConvert aiModelConvert;
    private final SysAuditLogService sysAuditLogService;
    private final TwoFactorService twoFactorService;
    private final SuperAdminVerifier superAdminVerifier;

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

    @Override
    public AiChannelAccountVO getAccount(Long channelConfigId, Long accountId) {
        verifyChannelExists(channelConfigId);
        AiChannelAccount account = findAndVerifyOwnership(channelConfigId, accountId);
        return toMaskedVO(account);
    }

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

        recordAuditLog(operatorId, channelConfigId, "创建账号: " + account.getAccountName());
        return toMaskedVO(account);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long channelConfigId, Long accountId, Long operatorId) {
        verifyChannelExists(channelConfigId);
        AiChannelAccount account = findAndVerifyOwnership(channelConfigId, accountId);
        aiChannelAccountRepository.removeById(accountId);

        recordAuditLog(operatorId, channelConfigId,
                "删除账号: " + account.getAccountName());
    }

    private AiChannelAccount findAndVerifyOwnership(Long channelConfigId, Long accountId) {
        AiChannelAccount account = ExceptionThrowerCore.requireNonNull(
                aiChannelAccountRepository.getById(accountId), ResultErrorCode.AI_ACCOUNT_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(
                !channelConfigId.equals(account.getChannelConfigId()),
                ResultErrorCode.AI_ACCOUNT_NOT_FOUND);
        return account;
    }

    private void verifyChannelExists(Long channelConfigId) {
        AiChannelConfig config = aiChannelConfigRepository.getById(channelConfigId);
        ExceptionThrowerCore.throwBusinessIfNull(config, ResultErrorCode.AI_CHANNEL_NOT_FOUND);
    }

    private AiChannelAccountVO toMaskedVO(AiChannelAccount account) {
        AiChannelAccountVO vo = aiModelConvert.toChannelAccountVO(account);
        if (vo != null && vo.getApiKeyEncrypted() != null && vo.getApiKeyEncrypted().length() > 7) {
            vo.setApiKeyEncrypted(AiChannelConfigAdminServiceImpl.maskApiKey(vo.getApiKeyEncrypted()));
        }
        return vo;
    }

    private void auditApiKeyChange(AiChannelAccount existing, AiChannelAccountSaveRequest request, Long operatorId) {
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
