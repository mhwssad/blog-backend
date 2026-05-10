package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.dto.mapper.ai.AiAgentTaskMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AiAgentTask Repository 实现。
 */
@Repository
public class AiAgentTaskRepositoryImpl
        extends ServiceImpl<AiAgentTaskMapper, AiAgentTask>
        implements AiAgentTaskRepository {

    @Override
    public Page<AiAgentTask> pageByUserIdAndStatus(Page<AiAgentTask> page, Long userId, Integer status) {
        LambdaQueryWrapper<AiAgentTask> wrapper = new LambdaQueryWrapper<AiAgentTask>()
                .eq(AiAgentTask::getUserId, userId)
                .eq(Optional.ofNullable(status).isPresent(), AiAgentTask::getStatus, status)
                .orderByDesc(AiAgentTask::getId);
        return page(page, wrapper);
    }

    @Override
    public Page<AiAgentTask> pageByAgentIdAndStatus(Page<AiAgentTask> page, Long agentId, Integer status) {
        LambdaQueryWrapper<AiAgentTask> wrapper = new LambdaQueryWrapper<AiAgentTask>()
                .eq(Optional.ofNullable(agentId).isPresent(), AiAgentTask::getAgentId, agentId)
                .eq(Optional.ofNullable(status).isPresent(), AiAgentTask::getStatus, status)
                .orderByDesc(AiAgentTask::getId);
        return page(page, wrapper);
    }

    @Override
    public long countByStatus(Integer status) {
        return count(new LambdaQueryWrapper<AiAgentTask>()
                .eq(AiAgentTask::getStatus, status));
    }
}
