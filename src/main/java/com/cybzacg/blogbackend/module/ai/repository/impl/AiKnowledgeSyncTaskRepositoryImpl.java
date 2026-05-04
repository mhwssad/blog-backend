package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeSyncTask;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSyncTaskStatusEnum;
import com.cybzacg.blogbackend.mapper.ai.AiKnowledgeSyncTaskMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiKnowledgeSyncTaskRepository;
import org.springframework.stereotype.Repository;

/**
 * AiKnowledgeSyncTask Repository 实现。
 */
@Repository
public class AiKnowledgeSyncTaskRepositoryImpl
        extends ServiceImpl<AiKnowledgeSyncTaskMapper, AiKnowledgeSyncTask>
        implements AiKnowledgeSyncTaskRepository {

    @Override
    public AiKnowledgeSyncTask findLatestRunningBySourceType(String sourceType) {
        return getOne(new LambdaQueryWrapper<AiKnowledgeSyncTask>()
                .eq(AiKnowledgeSyncTask::getSourceType, sourceType)
                .eq(AiKnowledgeSyncTask::getStatus, AiKnowledgeSyncTaskStatusEnum.RUNNING.getValue())
                .orderByDesc(AiKnowledgeSyncTask::getId)
                .last("limit 1"), false);
    }

    @Override
    public Page<AiKnowledgeSyncTask> pageByQuery(String sourceType, Integer status,
                                                  long current, long size) {
        LambdaQueryWrapper<AiKnowledgeSyncTask> wrapper = new LambdaQueryWrapper<AiKnowledgeSyncTask>()
                .eq(sourceType != null, AiKnowledgeSyncTask::getSourceType, sourceType)
                .eq(status != null, AiKnowledgeSyncTask::getStatus, status)
                .orderByDesc(AiKnowledgeSyncTask::getId);
        return page(new Page<>(current, size), wrapper);
    }
}
