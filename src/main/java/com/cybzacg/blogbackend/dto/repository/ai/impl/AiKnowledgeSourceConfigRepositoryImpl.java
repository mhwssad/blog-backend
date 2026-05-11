package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeSourceConfig;
import com.cybzacg.blogbackend.dto.mapper.ai.AiKnowledgeSourceConfigMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeSourceConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AiKnowledgeSourceConfig Repository 实现。
 */
@Repository
public class AiKnowledgeSourceConfigRepositoryImpl
        extends ServiceImpl<AiKnowledgeSourceConfigMapper, AiKnowledgeSourceConfig>
        implements AiKnowledgeSourceConfigRepository {

    @Override
    public AiKnowledgeSourceConfig findBySourceType(String sourceType) {
        return getOne(new LambdaQueryWrapper<AiKnowledgeSourceConfig>()
                .eq(AiKnowledgeSourceConfig::getSourceType, sourceType)
                .last("limit 1"), false);
    }

    @Override
    public List<AiKnowledgeSourceConfig> listEnabled() {
        return list(new LambdaQueryWrapper<AiKnowledgeSourceConfig>()
                .eq(AiKnowledgeSourceConfig::getEnabled, 1)
                .orderByAsc(AiKnowledgeSourceConfig::getId));
    }
}
