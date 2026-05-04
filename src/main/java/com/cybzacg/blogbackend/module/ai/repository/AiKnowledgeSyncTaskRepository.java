package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeSyncTask;

/**
 * AiKnowledgeSyncTask Repository。
 */
public interface AiKnowledgeSyncTaskRepository extends IService<AiKnowledgeSyncTask> {

    /**
     * 查询指定来源类型是否有正在执行中的任务。
     */
    AiKnowledgeSyncTask findLatestRunningBySourceType(String sourceType);

    /**
     * 按条件分页查询同步任务。
     *
     * @param sourceType 来源类型（可为 null）
     * @param status     状态（可为 null）
     * @param current    页码
     * @param size       每页条数
     * @return 分页结果
     */
    Page<AiKnowledgeSyncTask> pageByQuery(String sourceType, Integer status,
                                           long current, long size);
}
