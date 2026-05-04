package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;

/**
 * AI Agent 定义后台管理服务接口。
 */
public interface AiAgentDefinitionAdminService {

    /**
     * 分页查询 agent 定义。
     */
    PageResult<AiAgentDefinitionVO> pageDefinitions(AiAgentDefinitionPageQuery query);

    /**
     * 查询 agent 定义详情。
     */
    AiAgentDefinitionVO getDefinition(Long id);

    /**
     * 创建 agent 定义。
     */
    AiAgentDefinitionVO createDefinition(AiAgentDefinitionSaveRequest request, Long operatorId);

    /**
     * 更新 agent 定义。
     */
    AiAgentDefinitionVO updateDefinition(Long id, AiAgentDefinitionSaveRequest request, Long operatorId);

    /**
     * 切换 agent 启停状态。
     */
    void toggleEnabled(Long id, Integer enabled, Long operatorId);

    /**
     * 删除 agent 定义。
     */
    void deleteDefinition(Long id);
}
