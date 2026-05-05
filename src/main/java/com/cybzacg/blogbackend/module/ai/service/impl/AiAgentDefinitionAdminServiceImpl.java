package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.enums.ai.AiDataScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.constant.AiConstants;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.service.AiAgentDefinitionAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * AI Agent 定义后台管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentDefinitionAdminServiceImpl implements AiAgentDefinitionAdminService {

    private final AiAgentDefinitionRepository aiAgentDefinitionRepository;
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
        validateDataScopeJson(request.getDataScopeJson());

        AiAgentDefinition definition = aiModelConvert.toAgentDefinition(request);
        definition.setEnabled(1);
        definition.setMaxTurns(Optional.ofNullable(request.getMaxTurns()).orElse(AiConstants.DEFAULT_AGENT_MAX_TURNS));
        definition.setCreatedBy(operatorId);
        definition.setUpdatedBy(operatorId);
        aiAgentDefinitionRepository.save(definition);

        log.info("创建 Agent 定义: id={}, name={}", definition.getId(), definition.getName());
        return aiModelConvert.toAgentDefinitionVO(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgentDefinitionVO updateDefinition(Long id, AiAgentDefinitionSaveRequest request, Long operatorId) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(id),
                ResultErrorCode.AI_AGENT_NOT_FOUND);

        validateNameUnique(request.getName(), id);
        validateDataScopeJson(request.getDataScopeJson());

        aiModelConvert.updateAgentDefinition(request, definition);
        definition.setUpdatedBy(operatorId);
        aiAgentDefinitionRepository.updateById(definition);

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

        definition.setEnabled(enabled);
        definition.setUpdatedBy(operatorId);
        aiAgentDefinitionRepository.updateById(definition);

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

    /**
     * 校验 Agent 数据读取范围，确保只使用 AI 统一数据范围枚举声明的能力。
     */
    private void validateDataScopeJson(String dataScopeJson) {
        if (!StringUtils.hasText(dataScopeJson)) {
            return;
        }
        List<String> scopes;
        try {
            scopes = JsonUtils.getObjectMapper().readValue(dataScopeJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.ILLEGAL_ARGUMENT, "Agent 数据读取范围必须是字符串数组 JSON");
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(
                scopes == null || scopes.stream().anyMatch(scope -> !isValidDataScope(scope)),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "Agent 数据读取范围包含未知配置");
    }

    private boolean isValidDataScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return false;
        }
        if (AiDataScopeEnum.fromCode(scope) != null) {
            return true;
        }
        try {
            AiDataScopeEnum.valueOf(scope);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
