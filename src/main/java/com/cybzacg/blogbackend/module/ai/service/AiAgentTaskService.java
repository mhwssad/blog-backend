package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskVO;

/**
 * AI Agent 任务用户侧服务接口。
 */
public interface AiAgentTaskService {

    /**
     * 用户发起 agent 任务。
     */
    AiAgentTaskVO createTask(Long userId, AiAgentTaskCreateRequest request);

    /**
     * 用户分页查询自己的 agent 任务。
     */
    PageResult<AiAgentTaskVO> pageMyTasks(Long userId, AiAgentTaskPageQuery query);

    /**
     * 用户查询任务详情。
     */
    AiAgentTaskVO getTask(Long userId, Long taskId);

    /**
     * 用户取消任务（仅 PENDING 状态可取消）。
     */
    void cancelTask(Long userId, Long taskId);
}
