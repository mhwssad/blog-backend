package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;

/**
 * AI Agent 定义后台管理服务接口。
 *
 * <p>负责 Agent 定义的增删改查及启停管理，供后台管理员维护可用的 Agent 列表。
 */
public interface AiAgentDefinitionAdminService {

    /**
     * 分页查询 agent 定义。
     *
     * @param query 分页查询条件（页码、每页条数、关键词等）
     * @return 分页结果
     */
    PageResult<AiAgentDefinitionVO> pageDefinitions(AiAgentDefinitionPageQuery query);

    /**
     * 查询 agent 定义详情。
     *
     * @param id Agent 定义ID
     * @return Agent 定义详情
     */
    AiAgentDefinitionVO getDefinition(Long id);

    /**
     * 创建 agent 定义。
     *
     * @param request    创建请求（含名称、描述、系统提示词、关联工具等）
     * @param operatorId 操作人ID
     * @return 创建后的 Agent 定义视图对象
     */
    AiAgentDefinitionVO createDefinition(AiAgentDefinitionSaveRequest request, Long operatorId);

    /**
     * 更新 agent 定义。
     *
     * @param id         Agent 定义ID
     * @param request    更新请求
     * @param operatorId 操作人ID
     * @return 更新后的 Agent 定义视图对象
     */
    AiAgentDefinitionVO updateDefinition(Long id, AiAgentDefinitionSaveRequest request, Long operatorId);

    /**
     * 切换 agent 启停状态。
     *
     * @param id         Agent 定义ID
     * @param enabled    目标状态（1-启用，0-停用）
     * @param operatorId 操作人ID
     */
    void toggleEnabled(Long id, Integer enabled, Long operatorId);

    /**
     * 删除 agent 定义。
     *
     * @param id Agent 定义ID
     */
    void deleteDefinition(Long id);
}
