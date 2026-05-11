package com.cybzacg.blogbackend.dto.repository.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.dto.mapper.ai.AiAgentDefinitionMapper;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentDefinitionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AiAgentDefinition Repository 实现。
 */
@Repository
public class AiAgentDefinitionRepositoryImpl
        extends ServiceImpl<AiAgentDefinitionMapper, AiAgentDefinition>
        implements AiAgentDefinitionRepository {

    @Override
    public AiAgentDefinition findByName(String name) {
        return getOne(new LambdaQueryWrapper<AiAgentDefinition>()
                .eq(AiAgentDefinition::getName, name)
                .last("limit 1"), false);
    }

    @Override
    public List<AiAgentDefinition> listEnabled() {
        return list(new LambdaQueryWrapper<AiAgentDefinition>()
                .eq(AiAgentDefinition::getEnabled, 1)
                .orderByAsc(AiAgentDefinition::getId));
    }
}
