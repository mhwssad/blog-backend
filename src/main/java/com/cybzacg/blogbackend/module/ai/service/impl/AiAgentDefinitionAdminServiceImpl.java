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

    /**
     * 分页查询 Agent 定义列表，支持按名称关键词和启用状态过滤。
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
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

    /**
     * 根据 ID 获取 Agent 定义详情。
     *
     * @param id Agent 定义 ID
     * @return Agent 定义 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 定义不存在时抛出
     */
    @Override
    public AiAgentDefinitionVO getDefinition(Long id) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(id),
                ResultErrorCode.AI_AGENT_NOT_FOUND);
        return aiModelConvert.toAgentDefinitionVO(definition);
    }

    /**
     * 创建 Agent 定义，校验名称唯一性后持久化并记录高风险变更审计日志。
     *
     * @param request    创建请求
     * @param operatorId 操作人 ID
     * @return 创建后的 Agent 定义 VO
     */
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

    /**
     * 更新 Agent 定义，校验名称唯一性后更新并记录高风险变更审计日志。
     *
     * @param id         Agent 定义 ID
     * @param request    更新请求
     * @param operatorId 操作人 ID
     * @return 更新后的 Agent 定义 VO
     */
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

    /**
     * 切换 Agent 定义的启用/停用状态，状态实际变更时记录审计日志。
     *
     * @param id         Agent 定义 ID
     * @param enabled    目标状态（0=停用，1=启用）
     * @param operatorId 操作人 ID
     */
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

    /**
     * 物理删除 Agent 定义。
     *
     * @param id Agent 定义 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefinition(Long id) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(id),
                ResultErrorCode.AI_AGENT_NOT_FOUND);

        aiAgentDefinitionRepository.removeById(id);
        log.info("删除 Agent 定义: id={}, name={}", id, definition.getName());
    }

    /**
     * 校验 Agent 名称唯一性，排除指定 ID（用于更新场景排除自身）。
     *
     * @param name      待校验名称
     * @param excludeId 需要排除的 ID，新建时传 null
     */
    private void validateNameUnique(String name, Long excludeId) {
        AiAgentDefinition existing = aiAgentDefinitionRepository.findByName(name);
        if (existing != null && !existing.getId().equals(excludeId)) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.AI_AGENT_NAME_DUPLICATE);
        }
    }

    /**
     * 检测并记录高风险变更审计日志。
     * 高风险条件：新建、dataScopeJson 变更、extraConfigJson 变更或 systemPrompt 变更。
     *
     * @param before     变更前的定义快照，新建时为 null
     * @param after      变更后的定义
     * @param operatorId 操作人 ID
     */
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

    /**
     * 记录 Agent 定义变更审计日志。
     *
     * @param operatorId  操作人 ID
     * @param targetId    目标 Agent 定义 ID
     * @param beforeState 变更前状态摘要
     * @param afterState  变更后状态摘要
     */
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

    /**
     * 构建 Agent 定义状态摘要用于审计日志，包含名称、启用状态、数据范围和扩展配置。
     *
     * @param definition Agent 定义，为 null 时返回 null
     * @return 状态摘要字符串
     */
    private String buildStateSummary(AiAgentDefinition definition) {
        if (definition == null) {
            return null;
        }
        return "name=" + definition.getName()
                + ",enabled=" + definition.getEnabled()
                + ",dataScopeJson=" + definition.getDataScopeJson()
                + ",extraConfigJson=" + definition.getExtraConfigJson();
    }

    /**
     * 复制 Agent 定义的关键字段作为变更前快照，用于审计对比。
     * 仅复制参与高风险判断的字段：name、systemPrompt、dataScopeJson、enabled、extraConfigJson。
     *
     * @param source 源定义
     * @return 包含关键字段的副本
     */
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
