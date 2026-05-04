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
     */
    PageResult<AiAgentTaskAdminVO> pageTasks(AiAgentTaskAdminPageQuery query);

    /**
     * 后台查询任务详情。
     */
    AiAgentTaskAdminVO getTask(Long id);
}
