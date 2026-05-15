package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.common.constant.AiConstants;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeSourceConfig;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeSourceConfigRepository;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSourceConfigVO;
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

    /**
     * 查询所有知识源配置，按 ID 升序排列。
     *
     * @return 知识源配置 VO 列表
     */
    @Override
    public List<AiKnowledgeSourceConfigVO> listConfigs() {
        List<AiKnowledgeSourceConfig> configs = aiKnowledgeSourceConfigRepository.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiKnowledgeSourceConfig>()
                        .orderByAsc(AiKnowledgeSourceConfig::getId));
        return configs.stream().map(aiModelConvert::toKnowledgeSourceConfigVO).toList();
    }

    /**
     * 获取指定知识源配置的详情。
     *
     * @param id 配置 ID
     * @return 知识源配置 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 配置不存在时抛出
     */
    @Override
    public AiKnowledgeSourceConfigVO getConfig(Long id) {
        AiKnowledgeSourceConfig config = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSourceConfigRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_SOURCE_CONFIG_NOT_FOUND);
        return aiModelConvert.toKnowledgeSourceConfigVO(config);
    }

    /**
     * 更新知识源配置字段（同步间隔、过滤规则等）。
     *
     * @param id         配置 ID
     * @param request    更新请求
     * @param operatorId 操作人 ID
     * @return 更新后的配置 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 配置不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeSourceConfigVO updateConfig(Long id, AiKnowledgeSourceConfigSaveRequest request, Long operatorId) {
        AiKnowledgeSourceConfig config = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSourceConfigRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_SOURCE_CONFIG_NOT_FOUND);

        aiModelConvert.updateKnowledgeSourceConfig(request, config);
        config.setUpdatedBy(operatorId);
        aiKnowledgeSourceConfigRepository.updateById(config);
        log.info("更新知识源配置: id={}, operatorId={}", id, operatorId);

        return aiModelConvert.toKnowledgeSourceConfigVO(config);
    }

    /**
     * 切换知识源的启停状态。
     *
     * @param id         配置 ID
     * @param enabled    目标状态，0=禁用，1=启用
     * @param operatorId 操作人 ID
     * @throws com.cybzacg.blogbackend.exception.BusinessException 状态值非法或配置不存在时抛出
     */
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
        log.info("知识源配置启停状态变更: id={}, enabled={}, operatorId={}", id, enabled, operatorId);
    }

    /**
     * 初始化所有知识源类型的默认配置。
     *
     * <p>遍历 {@link AiKnowledgeSourceTypeEnum} 中的所有枚举值，
     * 为尚未创建配置的类型创建默认记录（默认启用、使用默认同步间隔）。
     * 通常在应用启动或首次部署时调用。
     */
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
