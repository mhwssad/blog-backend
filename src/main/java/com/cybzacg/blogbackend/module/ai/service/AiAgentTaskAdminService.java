package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminVO;

/**
 * AI Agent 任务后台管理服务接口。
 */
public interface AiAgentTaskAdminService {

    /**
     * 后台分页查询 agent 任务。
     *
     * @param query 分页查询条件（页码、每页条数、状态筛选等）
     * @return 分页结果
     */
    PageResult<AiAgentTaskAdminVO> pageTasks(AiAgentTaskAdminPageQuery query);

    /**
     * 后台查询任务详情。
     *
     * @param id 任务ID
     * @return 任务管理详情视图对象
     */
    AiAgentTaskAdminVO getTask(Long id);
}
