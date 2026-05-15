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
     *
     * @param userId  当前登录用户ID
     * @param request 任务创建请求（含 agent 定义ID、输入参数等）
     * @return 创建成功后的任务视图对象
     */
    AiAgentTaskVO createTask(Long userId, AiAgentTaskCreateRequest request);

    /**
     * 用户分页查询自己的 agent 任务。
     *
     * @param userId 当前登录用户ID
     * @param query  分页查询条件（页码、每页条数、状态筛选等）
     * @return 分页结果
     */
    PageResult<AiAgentTaskVO> pageMyTasks(Long userId, AiAgentTaskPageQuery query);

    /**
     * 用户查询任务详情。
     *
     * @param userId  当前登录用户ID
     * @param taskId  任务ID
     * @return 任务详情视图对象
     */
    AiAgentTaskVO getTask(Long userId, Long taskId);

    /**
     * 用户取消任务（仅 PENDING 状态可取消）。
     *
     * @param userId 当前登录用户ID
     * @param taskId 任务ID
     */
    void cancelTask(Long userId, Long taskId);
}
