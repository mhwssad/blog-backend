package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.ai.AiChannelStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiDataScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelConfigVO;
import com.cybzacg.blogbackend.module.ai.service.AiChannelConfigAdminService;
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
 * AI 渠道配置后台管理服务实现。
 *
 * <p>负责渠道配置的增删改查、API Key 脱敏、状态切换与审计日志记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChannelConfigAdminServiceImpl implements AiChannelConfigAdminService {

    private final AiChannelConfigRepository aiChannelConfigRepository;
    private final AiModelConvert aiModelConvert;
    private final SysAuditLogService sysAuditLogService;
    private final TwoFactorService twoFactorService;
    private final SuperAdminVerifier superAdminVerifier;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AiChannelConfigVO> listChannels(long current, long size) {
        current = PaginationUtils.normalizeCurrent(current);
        size = PaginationUtils.normalizeSize(size, 10L, 100L);

        Page<AiChannelConfig> page = aiChannelConfigRepository.page(
                new Page<>(current, size));
        List<AiChannelConfigVO> records = page.getRecords().stream()
                .map(this::toMaskedVO)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AiChannelConfigVO getChannel(Long id) {
        AiChannelConfig config = ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(id), ResultErrorCode.AI_CHANNEL_NOT_FOUND);
        return toMaskedVO(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChannelConfigVO createChannel(AiChannelConfigSaveRequest request, Long operatorId) {
        // 渠道编码唯一性校验
        AiChannelConfig existing = aiChannelConfigRepository.findByChannelCode(request.getChannelCode());
        ExceptionThrowerCore.throwBusinessIfNotNull(existing, ResultErrorCode.AI_CHANNEL_CODE_DUPLICATE);

        AiChannelConfig config = aiModelConvert.toChannelConfig(request);
        config.setCreatedBy(operatorId);
        config.setUpdatedBy(operatorId);
        aiChannelConfigRepository.save(config);
        log.info("创建 AI 渠道配置: id={}, channelCode={}, operatorId={}", config.getId(), config.getChannelCode(), operatorId);

        return toMaskedVO(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChannelConfigVO updateChannel(Long id, AiChannelConfigSaveRequest request, Long operatorId) {
        AiChannelConfig config = ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(id), ResultErrorCode.AI_CHANNEL_NOT_FOUND);

        // 如果渠道编码变更，检查唯一性
        if (!config.getChannelCode().equals(request.getChannelCode())) {
            AiChannelConfig existing = aiChannelConfigRepository.findByChannelCode(request.getChannelCode());
            ExceptionThrowerCore.throwBusinessIfNotNull(existing, ResultErrorCode.AI_CHANNEL_CODE_DUPLICATE);
        }

        // 审计高风险变更
        auditHighRiskChanges(config, request, operatorId);

        aiModelConvert.updateChannelConfig(request, config);
        config.setUpdatedBy(operatorId);
        aiChannelConfigRepository.updateById(config);
        log.info("更新 AI 渠道配置: id={}, channelCode={}, operatorId={}", id, config.getChannelCode(), operatorId);

        return toMaskedVO(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, Long operatorId) {
        AiChannelConfig config = ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(id), ResultErrorCode.AI_CHANNEL_NOT_FOUND);

        config.setStatus(status);
        config.setUpdatedBy(operatorId);
        aiChannelConfigRepository.updateById(config);
        log.info("切换 AI 渠道状态: id={}, status={}, operatorId={}", id, status, operatorId);

        // 审计
        recordAuditLog(operatorId, id, String.valueOf(config.getStatus()), String.valueOf(status));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(Long id, Long operatorId) {
        AiChannelConfig config = ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(id), ResultErrorCode.AI_CHANNEL_NOT_FOUND);

        // 软删除：状态设为停用
        config.setStatus(AiChannelStatusEnum.DISABLED.getValue());
        config.setUpdatedBy(operatorId);
        aiChannelConfigRepository.updateById(config);
        log.info("软删除 AI 渠道配置: id={}, operatorId={}", id, operatorId);

        // 审计
        recordAuditLog(operatorId, id, String.valueOf(AiChannelStatusEnum.ENABLED.getValue()),
                String.valueOf(AiChannelStatusEnum.DISABLED.getValue()));
    }

    /**
     * 将实体转换为 VO。
     */
    private AiChannelConfigVO toMaskedVO(AiChannelConfig config) {
        return aiModelConvert.toChannelConfigVO(config);
    }

    /**
     * API Key 脱敏：保留前 3 位和后 4 位。
     */
    static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 7) {
            return "****";
        }
        return apiKey.substring(0, 3) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 对高风险字段变更记录审计日志。
     */
    private void auditHighRiskChanges(AiChannelConfig existing, AiChannelConfigSaveRequest request, Long operatorId) {
        boolean statusChanged = request.getStatus() != null
                && !request.getStatus().equals(existing.getStatus());
        boolean dataScopePrivateChat = request.getDataScopeJson() != null
                && AiDataScopeEnum.isHighRisk(request.getDataScopeJson());

        if (statusChanged || dataScopePrivateChat) {
            superAdminVerifier.requireSuperAdmin(operatorId);
            ExceptionThrowerCore.throwBusinessIfNot(
                    twoFactorService.validateTicket(request.getMfaTicket(), operatorId),
                    ResultErrorCode.MFA_TICKET_INVALID);

            SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
            auditRequest.setOperatorUserId(operatorId);
            auditRequest.setOperationType(SysAuditOperationType.MODIFY_AI_CONFIG.getCode());
            auditRequest.setTargetTypeName("AiChannelConfig");
            auditRequest.setTargetId(existing.getId());
            auditRequest.setBeforeState(buildStateSummary(existing));
            auditRequest.setAfterState("statusChanged=" + statusChanged
                    + ",privateChatEnabled=" + dataScopePrivateChat);
            sysAuditLogService.record(auditRequest);
            log.info("记录 AI 渠道高风险变更审计: channelId={}, statusChanged={}, privateChatEnabled={}, operatorId={}",
                    existing.getId(), statusChanged, dataScopePrivateChat, operatorId);
        }
    }

    /**
     * 构建渠道配置状态摘要用于审计日志。
     */
    private String buildStateSummary(AiChannelConfig config) {
        return "id=" + config.getId()
                + ",channelCode=" + config.getChannelCode()
                + ",status=" + config.getStatus()
                + ",isDefault=" + config.getIsDefault();
    }

    /**
     * 记录审计日志。
     */
    private void recordAuditLog(Long operatorId, Long targetId, String beforeState, String afterState) {
        SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
        auditRequest.setOperatorUserId(operatorId);
        auditRequest.setOperationType(SysAuditOperationType.MODIFY_AI_CONFIG.getCode());
        auditRequest.setTargetTypeName("AiChannelConfig");
        auditRequest.setTargetId(targetId);
        auditRequest.setBeforeState(beforeState);
        auditRequest.setAfterState(afterState);
        sysAuditLogService.record(auditRequest);
    }
}
