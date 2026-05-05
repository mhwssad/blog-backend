package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiToolDefinition;

/**
 * AI 工具定义 Repository。
 */
public interface AiToolDefinitionRepository extends IService<AiToolDefinition> {
    AiToolDefinition findByToolCode(String toolCode);
}
