package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.constant.AiConstants;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.service.AiAgentDefinitionAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
