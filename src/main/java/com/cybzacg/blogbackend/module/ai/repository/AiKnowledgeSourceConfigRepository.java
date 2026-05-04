package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeSourceConfig;

import java.util.List;

/**
 * AiKnowledgeSourceConfig Repository。
 */
public interface AiKnowledgeSourceConfigRepository extends IService<AiKnowledgeSourceConfig> {

    /**
     * 根据知识源类型查询配置。
     */
    AiKnowledgeSourceConfig findBySourceType(String sourceType);

    /**
     * 查询所有已启用的知识源配置。
     */
    List<AiKnowledgeSourceConfig> listEnabled();
}
