package com.cybzacg.blogbackend.dto.repository.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentTask;

/**
 * AiAgentTask Repository。
 */
public interface AiAgentTaskRepository extends IService<AiAgentTask> {

    /**
     * 按用户和状态分页查询任务。
     */
    Page<AiAgentTask> pageByUserIdAndStatus(Page<AiAgentTask> page, Long userId, Integer status);

    /**
     * 按 agent 定义和状态分页查询任务（后台）。
     */
    Page<AiAgentTask> pageByAgentIdAndStatus(Page<AiAgentTask> page, Long agentId, Integer status);

    /**
     * 按状态统计任务数。
     */
    long countByStatus(Integer status);
}
