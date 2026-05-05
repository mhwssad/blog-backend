package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.mapper.ai.AiToolDefinitionMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiToolDefinitionRepository;
import org.springframework.stereotype.Repository;

/**
 * AI 工具定义 Repository 实现。
 */
@Repository
public class AiToolDefinitionRepositoryImpl extends ServiceImpl<AiToolDefinitionMapper, AiToolDefinition>
        implements AiToolDefinitionRepository {

    @Override
    public AiToolDefinition findByToolCode(String toolCode) {
        return getOne(new LambdaQueryWrapper<AiToolDefinition>()
                .eq(AiToolDefinition::getToolCode, toolCode)
                .last("limit 1"), false);
    }
}
