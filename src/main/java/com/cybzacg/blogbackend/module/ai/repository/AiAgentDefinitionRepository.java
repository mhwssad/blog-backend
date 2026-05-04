package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;

import java.util.List;

/**
 * AiAgentDefinition Repository。
 */
public interface AiAgentDefinitionRepository extends IService<AiAgentDefinition> {

    /**
     * 根据 agent 名称查询定义。
     */
    AiAgentDefinition findByName(String name);

    /**
     * 查询所有已启用的 agent 定义。
     */
    List<AiAgentDefinition> listEnabled();
}
