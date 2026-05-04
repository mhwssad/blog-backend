package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.domain.ai.AiKnowledgeSourceConfig;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.constant.AiConstants;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigVO;
import com.cybzacg.blogbackend.module.ai.repository.AiKnowledgeSourceConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSourceConfigAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * AI 知识源配置后台管理服务实现。
 *
 * <p>负责知识源配置的查询、更新、启停管理和默认配置初始化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeSourceConfigAdminServiceImpl implements AiKnowledgeSourceConfigAdminService {

    private final AiKnowledgeSourceConfigRepository aiKnowledgeSourceConfigRepository;
    private final AiModelConvert aiModelConvert;

    @Override
    public List<AiKnowledgeSourceConfigVO> listConfigs() {
        List<AiKnowledgeSourceConfig> configs = aiKnowledgeSourceConfigRepository.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiKnowledgeSourceConfig>()
                        .orderByAsc(AiKnowledgeSourceConfig::getId));
        return configs.stream().map(aiModelConvert::toKnowledgeSourceConfigVO).toList();
    }

    @Override
    public AiKnowledgeSourceConfigVO getConfig(Long id) {
        AiKnowledgeSourceConfig config = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSourceConfigRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_SOURCE_CONFIG_NOT_FOUND);
        return aiModelConvert.toKnowledgeSourceConfigVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeSourceConfigVO updateConfig(Long id, AiKnowledgeSourceConfigSaveRequest request, Long operatorId) {
        AiKnowledgeSourceConfig config = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSourceConfigRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_SOURCE_CONFIG_NOT_FOUND);

        aiModelConvert.updateKnowledgeSourceConfig(request, config);
        config.setUpdatedBy(operatorId);
        aiKnowledgeSourceConfigRepository.updateById(config);

        return aiModelConvert.toKnowledgeSourceConfigVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long id, Integer enabled, Long operatorId) {
        ExceptionThrowerCore.throwBusinessIf(
                enabled == null || (enabled != 0 && enabled != 1),
                ResultErrorCode.ILLEGAL_ARGUMENT);

        AiKnowledgeSourceConfig config = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSourceConfigRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_SOURCE_CONFIG_NOT_FOUND);

        config.setEnabled(enabled);
        config.setUpdatedBy(operatorId);
        aiKnowledgeSourceConfigRepository.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultConfigs() {
        Arrays.stream(AiKnowledgeSourceTypeEnum.values()).forEach(type -> {
            AiKnowledgeSourceConfig existing = aiKnowledgeSourceConfigRepository.findBySourceType(type.getCode());
            if (existing == null) {
                AiKnowledgeSourceConfig config = new AiKnowledgeSourceConfig();
                config.setSourceType(type.getCode());
                config.setEnabled(1);
                config.setSyncInterval(AiConstants.DEFAULT_KNOWLEDGE_SYNC_INTERVAL);
                aiKnowledgeSourceConfigRepository.save(config);
                log.info("初始化知识源配置: {}", type.getCode());
            }
        });
    }
}
