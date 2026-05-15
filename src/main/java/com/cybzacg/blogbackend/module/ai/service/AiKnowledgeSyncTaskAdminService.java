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
     *
     * @param request    同步触发请求（含知识源类型、同步范围等）
     * @param operatorId 操作人ID
     * @return 创建的同步任务视图对象
     */
    AiKnowledgeSyncTaskVO triggerSync(AiKnowledgeSyncTriggerRequest request, Long operatorId);

    /**
     * 重试失败的同步任务。
     *
     * @param taskId     同步任务ID
     * @param operatorId 操作人ID
     */
    void retryTask(Long taskId, Long operatorId);

    /**
     * 分页查询同步任务。
     *
     * @param query 分页查询条件（页码、每页条数、状态筛选等）
     * @return 分页结果
     */
    PageResult<AiKnowledgeSyncTaskVO> listTasks(AiKnowledgeSyncTaskPageQuery query);

    /**
     * 查询同步任务详情。
     *
     * @param taskId 同步任务ID
     * @return 同步任务详情视图对象
     */
    AiKnowledgeSyncTaskVO getTask(Long taskId);
}
