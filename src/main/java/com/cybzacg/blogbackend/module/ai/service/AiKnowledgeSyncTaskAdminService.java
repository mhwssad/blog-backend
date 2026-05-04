package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTriggerRequest;

/**
 * AI 知识同步任务后台管理服务接口。
 *
 * <p>负责同步任务的触发、重试和查询。
 */
public interface AiKnowledgeSyncTaskAdminService {

    /**
     * 触发知识同步任务。
     */
    AiKnowledgeSyncTaskVO triggerSync(AiKnowledgeSyncTriggerRequest request, Long operatorId);

    /**
     * 重试失败的同步任务。
     */
    void retryTask(Long taskId, Long operatorId);

    /**
     * 分页查询同步任务。
     */
    PageResult<AiKnowledgeSyncTaskVO> listTasks(AiKnowledgeSyncTaskPageQuery query);

    /**
     * 查询同步任务详情。
     */
    AiKnowledgeSyncTaskVO getTask(Long taskId);
}
