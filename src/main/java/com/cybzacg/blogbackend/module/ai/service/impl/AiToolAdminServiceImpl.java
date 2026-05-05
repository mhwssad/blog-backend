package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiToolAuthorization;
import com.cybzacg.blogbackend.domain.ai.AiToolCallLog;
import com.cybzacg.blogbackend.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.ai.AiToolSourceTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiToolModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolAuthorizationVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolCallLogPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolCallLogVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolDefinitionVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiToolExecutionContext;
import com.cybzacg.blogbackend.module.ai.repository.AiToolAuthorizationRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolCallLogRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.service.AiToolAdminService;
import com.cybzacg.blogbackend.module.ai.service.AiToolExecutionService;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 后台 AI 工具管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiToolAdminServiceImpl implements AiToolAdminService {

    private final AiToolDefinitionRepository aiToolDefinitionRepository;
    private final AiToolAuthorizationRepository aiToolAuthorizationRepository;
    private final AiToolCallLogRepository aiToolCallLogRepository;
    private final AiToolExecutionService aiToolExecutionService;
    private final AiToolModelConvert aiToolModelConvert;
    private final SysAuditLogService sysAuditLogService;

    @Override
    public PageResult<AiToolDefinitionVO> pageTools(AiToolDefinitionPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Page<AiToolDefinition> page = aiToolDefinitionRepository.page(new Page<>(current, size),
                new LambdaQueryWrapper<AiToolDefinition>()
                        .eq(StringUtils.hasText(query.getToolCode()), AiToolDefinition::getToolCode, query.getToolCode())
                        .like(StringUtils.hasText(query.getToolName()), AiToolDefinition::getToolName, query.getToolName())
                        .eq(StringUtils.hasText(query.getSourceType()), AiToolDefinition::getSourceType, query.getSourceType())
                        .eq(query.getEnabled() != null, AiToolDefinition::getEnabled, query.getEnabled())
                        .orderByDesc(AiToolDefinition::getId));
        List<AiToolDefinitionVO> records = page.getRecords().stream()
                .map(aiToolModelConvert::toToolDefinitionVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public AiToolDefinitionVO getTool(Long id) {
        AiToolDefinition tool = getToolOrThrow(id);
        return aiToolModelConvert.toToolDefinitionVO(tool);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiToolDefinitionVO createTool(AiToolDefinitionSaveRequest request, Long operatorId) {
        validateToolRequest(request);
        ExceptionThrowerCore.throwBusinessIfNotNull(
                aiToolDefinitionRepository.findByToolCode(request.getToolCode()),
                ResultErrorCode.AI_TOOL_CODE_DUPLICATE);

        AiToolDefinition entity = aiToolModelConvert.toToolDefinition(request);
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        aiToolDefinitionRepository.save(entity);
        recordAudit(operatorId, SysAuditOperationType.MODIFY_AI_TOOL, "AiToolDefinition",
                entity.getId(), null, "create:" + entity.getToolCode());
        return aiToolModelConvert.toToolDefinitionVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiToolDefinitionVO updateTool(Long id, AiToolDefinitionSaveRequest request, Long operatorId) {
        validateToolRequest(request);
        AiToolDefinition entity = getToolOrThrow(id);
        AiToolDefinition existing = aiToolDefinitionRepository.findByToolCode(request.getToolCode());
        ExceptionThrowerCore.throwBusinessIf(existing != null && !existing.getId().equals(id),
                ResultErrorCode.AI_TOOL_CODE_DUPLICATE);

        String before = entity.getToolCode() + ":" + entity.getEnabled();
        aiToolModelConvert.updateToolDefinition(request, entity);
        entity.setUpdatedBy(operatorId);
        aiToolDefinitionRepository.updateById(entity);
        recordAudit(operatorId, SysAuditOperationType.MODIFY_AI_TOOL, "AiToolDefinition",
                entity.getId(), before, entity.getToolCode() + ":" + entity.getEnabled());
        return aiToolModelConvert.toToolDefinitionVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateToolStatus(Long id, Integer enabled, Long operatorId) {
        AiToolSupport.validateEnabled(enabled);
        AiToolDefinition entity = getToolOrThrow(id);
        Integer before = entity.getEnabled();
        entity.setEnabled(enabled);
        entity.setUpdatedBy(operatorId);
        aiToolDefinitionRepository.updateById(entity);
        recordAudit(operatorId, SysAuditOperationType.MODIFY_AI_TOOL, "AiToolDefinition",
                entity.getId(), String.valueOf(before), String.valueOf(enabled));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTool(Long id, Long operatorId) {
        AiToolDefinition entity = getToolOrThrow(id);
        entity.setEnabled(0);
        entity.setUpdatedBy(operatorId);
        aiToolDefinitionRepository.updateById(entity);
        recordAudit(operatorId, SysAuditOperationType.MODIFY_AI_TOOL, "AiToolDefinition",
                id, "enabled=1", "enabled=0");
    }

    @Override
    public AiToolExecuteVO executeTool(Long id, AiToolExecuteRequest request, Long operatorId) {
        AiToolDefinition tool = getToolOrThrow(id);
        AiToolExecutionContext context = AiToolExecutionContext.builder()
                .userId(operatorId)
                .agentId(request.getAgentId())
                .sessionId(request.getSessionId())
                .taskId(request.getTaskId())
                .sceneType(request.getSceneType())
                .dataScope(request.getDataScope())
                .authorities(SecurityUtils.getAuthoritySet())
                .build();
        return aiToolExecutionService.execute(tool.getToolCode(), request.getArguments(), context);
    }

    @Override
    public PageResult<AiToolCallLogVO> pageCallLogs(AiToolCallLogPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Page<AiToolCallLog> page = aiToolCallLogRepository.page(new Page<>(current, size),
                new LambdaQueryWrapper<AiToolCallLog>()
                        .eq(query.getToolId() != null, AiToolCallLog::getToolId, query.getToolId())
                        .eq(query.getUserId() != null, AiToolCallLog::getUserId, query.getUserId())
                        .eq(query.getAgentId() != null, AiToolCallLog::getAgentId, query.getAgentId())
                        .eq(query.getTaskId() != null, AiToolCallLog::getTaskId, query.getTaskId())
                        .eq(query.getSuccessStatus() != null, AiToolCallLog::getSuccessStatus, query.getSuccessStatus())
                        .orderByDesc(AiToolCallLog::getId));
        List<AiToolCallLogVO> records = page.getRecords().stream()
                .map(aiToolModelConvert::toToolCallLogVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public PageResult<AiToolAuthorizationVO> pageAuthorizations(AiToolAuthorizationPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Page<AiToolAuthorization> page = aiToolAuthorizationRepository.page(new Page<>(current, size),
                new LambdaQueryWrapper<AiToolAuthorization>()
                        .eq(query.getToolId() != null, AiToolAuthorization::getToolId, query.getToolId())
                        .eq(StringUtils.hasText(query.getAuthorizationType()),
                                AiToolAuthorization::getAuthorizationType, query.getAuthorizationType())
                        .eq(StringUtils.hasText(query.getAuthorizationKey()),
                                AiToolAuthorization::getAuthorizationKey, query.getAuthorizationKey())
                        .eq(query.getEnabled() != null, AiToolAuthorization::getEnabled, query.getEnabled())
                        .orderByDesc(AiToolAuthorization::getId));
        List<AiToolAuthorizationVO> records = page.getRecords().stream()
                .map(aiToolModelConvert::toToolAuthorizationVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiToolAuthorizationVO createAuthorization(AiToolAuthorizationSaveRequest request, Long operatorId) {
        validateAuthorizationRequest(request);
        getToolOrThrow(request.getToolId());
        AiToolAuthorization entity = aiToolModelConvert.toToolAuthorization(request);
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        aiToolAuthorizationRepository.save(entity);
        recordAudit(operatorId, SysAuditOperationType.MODIFY_AI_TOOL_AUTHORIZATION, "AiToolAuthorization",
                entity.getId(), null, "create:" + entity.getAuthorizationType() + ":" + entity.getAuthorizationKey());
        return aiToolModelConvert.toToolAuthorizationVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiToolAuthorizationVO updateAuthorization(Long id, AiToolAuthorizationSaveRequest request, Long operatorId) {
        validateAuthorizationRequest(request);
        getToolOrThrow(request.getToolId());
        AiToolAuthorization entity = ExceptionThrowerCore.requireNonNull(
                aiToolAuthorizationRepository.getById(id),
                ResultErrorCode.AI_TOOL_AUTHORIZATION_NOT_FOUND);
        String before = entity.getAuthorizationType() + ":" + entity.getAuthorizationKey() + ":" + entity.getEnabled();
        aiToolModelConvert.updateToolAuthorization(request, entity);
        entity.setUpdatedBy(operatorId);
        aiToolAuthorizationRepository.updateById(entity);
        recordAudit(operatorId, SysAuditOperationType.MODIFY_AI_TOOL_AUTHORIZATION, "AiToolAuthorization",
                id, before, entity.getAuthorizationType() + ":" + entity.getAuthorizationKey() + ":" + entity.getEnabled());
        return aiToolModelConvert.toToolAuthorizationVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAuthorization(Long id, Long operatorId) {
        AiToolAuthorization entity = ExceptionThrowerCore.requireNonNull(
                aiToolAuthorizationRepository.getById(id),
                ResultErrorCode.AI_TOOL_AUTHORIZATION_NOT_FOUND);
        aiToolAuthorizationRepository.removeById(id);
        recordAudit(operatorId, SysAuditOperationType.MODIFY_AI_TOOL_AUTHORIZATION, "AiToolAuthorization",
                id, entity.getAuthorizationType() + ":" + entity.getAuthorizationKey(), "deleted");
    }

    private AiToolDefinition getToolOrThrow(Long id) {
        return ExceptionThrowerCore.requireNonNull(
                aiToolDefinitionRepository.getById(id),
                ResultErrorCode.AI_TOOL_NOT_FOUND);
    }

    private void validateToolRequest(AiToolDefinitionSaveRequest request) {
        AiToolSupport.validateToolSource(request.getSourceType());
        AiToolSupport.validateRiskLevel(request.getRiskLevel());
        AiToolSupport.validateEnabled(request.getEnabled());
        AiToolSupport.validateJsonObjectOrBlank(request.getParametersSchema(),
                ResultErrorCode.AI_TOOL_SCHEMA_INVALID, "参数 Schema 必须是 JSON 对象");
        AiToolSupport.validateJsonObjectOrBlank(request.getResultSchema(),
                ResultErrorCode.AI_TOOL_SCHEMA_INVALID, "返回 Schema 必须是 JSON 对象");
        AiToolSupport.validateJsonArrayOfToolScopes(request.getUseScenarios());
        if (AiToolSourceTypeEnum.MCP.getCode().equalsIgnoreCase(request.getSourceType())) {
            ExceptionThrowerCore.throwBusinessIf(request.getMcpServerId() == null || !StringUtils.hasText(request.getMcpToolName()),
                    ResultErrorCode.ILLEGAL_ARGUMENT, "MCP 工具必须指定服务 ID 和原始工具名");
        }
    }

    private void validateAuthorizationRequest(AiToolAuthorizationSaveRequest request) {
        AiToolSupport.validateAuthorizationType(request.getAuthorizationType());
        AiToolSupport.validateEnabled(request.getEnabled());
        AiToolSupport.validateDataScope(request.getDataScope());
    }

    private void recordAudit(Long operatorId, SysAuditOperationType operationType, String targetType,
                             Long targetId, String before, String after) {
        SysAuditLogCreateRequest audit = new SysAuditLogCreateRequest();
        audit.setOperatorUserId(operatorId);
        audit.setOperationType(operationType.getCode());
        audit.setTargetTypeName(targetType);
        audit.setTargetId(targetId);
        audit.setBeforeState(before);
        audit.setAfterState(after);
        sysAuditLogService.record(audit);
    }
}
