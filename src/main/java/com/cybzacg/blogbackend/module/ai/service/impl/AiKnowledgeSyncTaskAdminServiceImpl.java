package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiKnowledgeSyncTask;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSyncTaskStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.constant.AiConstants;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTriggerRequest;
import com.cybzacg.blogbackend.module.ai.repository.AiKnowledgeSyncTaskRepository;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSyncTaskAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 知识同步任务后台管理服务实现。
 *
 * <p>负责同步任务的触发、重试、状态流转和查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeSyncTaskAdminServiceImpl implements AiKnowledgeSyncTaskAdminService {

    private final AiKnowledgeSyncTaskRepository aiKnowledgeSyncTaskRepository;
    private final AiModelConvert aiModelConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeSyncTaskVO triggerSync(AiKnowledgeSyncTriggerRequest request, Long operatorId) {
        String sourceType = request.getSourceType();
        ExceptionThrowerCore.throwBusinessIf(
                !AiKnowledgeSourceTypeEnum.contains(sourceType),
                ResultErrorCode.AI_KNOWLEDGE_SOURCE_TYPE_INVALID);

        // 同一来源类型不允许并发执行
        AiKnowledgeSyncTask runningTask = aiKnowledgeSyncTaskRepository
                .findLatestRunningBySourceType(sourceType);
        ExceptionThrowerCore.throwBusinessIf(
                runningTask != null,
                ResultErrorCode.AI_KNOWLEDGE_SYNC_ALREADY_RUNNING);

        AiKnowledgeSyncTask task = new AiKnowledgeSyncTask();
        task.setTaskType(request.getTaskType() != null ? request.getTaskType() : AiConstants.SYNC_TASK_TYPE_FULL);
        task.setSourceType(sourceType);
        task.setStatus(AiKnowledgeSyncTaskStatusEnum.PENDING.getValue());
        task.setRetryCount(0);
        task.setMaxRetry(AiConstants.DEFAULT_KNOWLEDGE_MAX_RETRY);
        task.setTriggeredBy(AiConstants.SYNC_TRIGGER_ADMIN);
        task.setOperatorId(operatorId);
        task.setRemark(request.getRemark());
        aiKnowledgeSyncTaskRepository.save(task);

        log.info("创建知识同步任务: id={}, sourceType={}, taskType={}", task.getId(), sourceType, task.getTaskType());
        return aiModelConvert.toKnowledgeSyncTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Long taskId, Long operatorId) {
        AiKnowledgeSyncTask task = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSyncTaskRepository.getById(taskId),
                ResultErrorCode.AI_KNOWLEDGE_SYNC_TASK_NOT_FOUND);

        ExceptionThrowerCore.throwBusinessIf(
                !AiKnowledgeSyncTaskStatusEnum.FAILED.getValue().equals(task.getStatus()),
                ResultErrorCode.AI_KNOWLEDGE_SYNC_TASK_FAILED,
                "仅失败任务可重试");

        ExceptionThrowerCore.throwBusinessIf(
                task.getRetryCount() >= task.getMaxRetry(),
                ResultErrorCode.AI_KNOWLEDGE_SYNC_RETRY_EXCEEDED);

        task.setStatus(AiKnowledgeSyncTaskStatusEnum.PENDING.getValue());
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        task.setOperatorId(operatorId);
        aiKnowledgeSyncTaskRepository.updateById(task);

        log.info("重试知识同步任务: id={}, retryCount={}", task.getId(), task.getRetryCount());
    }

    @Override
    public PageResult<AiKnowledgeSyncTaskVO> listTasks(AiKnowledgeSyncTaskPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);

        Page<AiKnowledgeSyncTask> page = aiKnowledgeSyncTaskRepository.pageByQuery(
                query.getSourceType(), query.getStatus(), current, size);

        List<AiKnowledgeSyncTaskVO> records = page.getRecords().stream()
                .map(aiModelConvert::toKnowledgeSyncTaskVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public AiKnowledgeSyncTaskVO getTask(Long taskId) {
        AiKnowledgeSyncTask task = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSyncTaskRepository.getById(taskId),
                ResultErrorCode.AI_KNOWLEDGE_SYNC_TASK_NOT_FOUND);
        return aiModelConvert.toKnowledgeSyncTaskVO(task);
    }
}
