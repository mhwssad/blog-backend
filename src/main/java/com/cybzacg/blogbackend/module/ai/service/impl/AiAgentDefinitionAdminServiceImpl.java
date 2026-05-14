package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.constant.AiConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;
import com.cybzacg.blogbackend.module.ai.service.AiAgentDefinitionAdminService;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * AI Agent 定义后台管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentDefinitionAdminServiceImpl implements AiAgentDefinitionAdminService {

    private final AiAgentDefinitionRepository aiAgentDefinitionRepository;
    private final SysAuditLogService sysAuditLogService;
    private final AiModelConvert aiModelConvert;

    @Override
    public PageResult<AiAgentDefinitionVO> pageDefinitions(AiAgentDefinitionPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Page<AiAgentDefinition> page = new Page<>(current, size);
        LambdaQueryWrapper<AiAgentDefinition> wrapper = new LambdaQueryWrapper<AiAgentDefinition>()
                .like(Optional.ofNullable(query.getKeyword()).isPresent(),
                        AiAgentDefinition::getName, query.getKeyword())
                .eq(Optional.ofNullable(query.getEnabled()).isPresent(),
                        AiAgentDefinition::getEnabled, query.getEnabled())
                .orderByDesc(AiAgentDefinition::getId);
        Page<AiAgentDefinition> result = aiAgentDefinitionRepository.page(page, wrapper);
        List<AiAgentDefinitionVO> voList = result.getRecords().stream()
                .map(aiModelConvert::toAgentDefinitionVO)
                .toList();
        return PageResult.of(result, voList);
    }

    @Override
    public AiAgentDefinitionVO getDefinition(Long id) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(id),
                ResultErrorCode.AI_AGENT_NOT_FOUND);
        return aiModelConvert.toAgentDefinitionVO(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgentDefinitionVO createDefinition(AiAgentDefinitionSaveRequest request, Long operatorId) {
        validateNameUnique(request.getName(), null);

        AiAgentDefinition definition = aiModelConvert.toAgentDefinition(request);
        definition.setEnabled(1);
        definition.setMaxTurns(Optional.ofNullable(request.getMaxTurns()).orElse(AiConstants.DEFAULT_AGENT_MAX_TURNS));
        definition.setCreatedBy(operatorId);
        definition.setUpdatedBy(operatorId);
        aiAgentDefinitionRepository.save(definition);
        recordHighRiskAudit(null, definition, operatorId);

        log.info("创建 Agent 定义: id={}, name={}", definition.getId(), definition.getName());
        return aiModelConvert.toAgentDefinitionVO(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgentDefinitionVO updateDefinition(Long id, AiAgentDefinitionSaveRequest request, Long operatorId) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(id),
                ResultErrorCode.AI_AGENT_NOT_FOUND);
        AiAgentDefinition before = copyDefinition(definition);

        validateNameUnique(request.getName(), id);

        aiModelConvert.updateAgentDefinition(request, definition);
        definition.setUpdatedBy(operatorId);
        aiAgentDefinitionRepository.updateById(definition);
        recordHighRiskAudit(before, definition, operatorId);

        log.info("更新 Agent 定义: id={}, name={}", id, definition.getName());
        return aiModelConvert.toAgentDefinitionVO(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long id, Integer enabled, Long operatorId) {
        ExceptionThrowerCore.throwBusinessIf(
                enabled == null || (enabled != 0 && enabled != 1),
                ResultErrorCode.ILLEGAL_ARGUMENT);

        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(id),
                ResultErrorCode.AI_AGENT_NOT_FOUND);
        Integer before = definition.getEnabled();

        definition.setEnabled(enabled);
        definition.setUpdatedBy(operatorId);
        aiAgentDefinitionRepository.updateById(definition);
        if (!Objects.equals(before, enabled)) {
            recordAudit(operatorId, definition.getId(), "enabled=" + before, "enabled=" + enabled);
        }

        log.info("切换 Agent 状态: id={}, enabled={}", id, enabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(Long id) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(id),
                ResultErrorCode.AI_AGENT_NOT_FOUND);

        aiAgentDefinitionRepository.removeById(id);
        log.info("删除 Agent 定义: id={}, name={}", id, definition.getName());
    }

    private void validateNameUnique(String name, Long excludeId) {
        AiAgentDefinition existing = aiAgentDefinitionRepository.findByName(name);
        if (existing != null && !existing.getId().equals(excludeId)) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.AI_AGENT_NAME_DUPLICATE);
        }
    }

    private void recordHighRiskAudit(AiAgentDefinition before, AiAgentDefinition after, Long operatorId) {
        boolean highRisk = before == null
                || !Objects.equals(before.getDataScopeJson(), after.getDataScopeJson())
                || !Objects.equals(before.getExtraConfigJson(), after.getExtraConfigJson())
                || !Objects.equals(before.getSystemPrompt(), after.getSystemPrompt());
        if (!highRisk) {
            return;
        }
        recordAudit(operatorId, after.getId(), buildStateSummary(before), buildStateSummary(after));
    }

    private void recordAudit(Long operatorId, Long targetId, String beforeState, String afterState) {
        SysAuditLogCreateRequest audit = new SysAuditLogCreateRequest();
        audit.setOperatorUserId(operatorId);
        audit.setOperationType(SysAuditOperationType.MODIFY_AI_AGENT.getCode());
        audit.setTargetTypeName("AiAgentDefinition");
        audit.setTargetId(targetId);
        audit.setBeforeState(beforeState);
        audit.setAfterState(afterState);
        sysAuditLogService.record(audit);
    }

    private String buildStateSummary(AiAgentDefinition definition) {
        if (definition == null) {
            return null;
        }
        return "name=" + definition.getName()
                + ",enabled=" + definition.getEnabled()
                + ",dataScopeJson=" + definition.getDataScopeJson()
                + ",extraConfigJson=" + definition.getExtraConfigJson();
    }

    private AiAgentDefinition copyDefinition(AiAgentDefinition source) {
        AiAgentDefinition target = new AiAgentDefinition();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setSystemPrompt(source.getSystemPrompt());
        target.setDataScopeJson(source.getDataScopeJson());
        target.setEnabled(source.getEnabled());
        target.setExtraConfigJson(source.getExtraConfigJson());
        return target;
    }
}
