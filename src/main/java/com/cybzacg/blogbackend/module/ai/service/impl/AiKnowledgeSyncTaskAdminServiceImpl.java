package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.constant.AiConstants;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeSyncTask;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeEntryRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeSyncTaskRepository;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeEntryStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSyncTaskStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTaskVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeSyncTriggerRequest;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeChunkService;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSourceExtractor;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeSyncTaskAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final AiKnowledgeEntryRepository aiKnowledgeEntryRepository;
    private final AiKnowledgeSourceExtractor aiKnowledgeSourceExtractor;
    private final AiKnowledgeChunkService aiKnowledgeChunkService;
    private final AiModelConvert aiModelConvert;

    /**
     * 手动触发知识同步任务。
     *
     * <p>同一来源类型不允许并发执行，若存在运行中的任务则抛出异常。
     * 任务创建后立即同步执行（抽取条目 → 分块 → 向量索引重建）。
     *
     * @param request    触发请求，包含来源类型、任务类型等参数
     * @param operatorId 操作人 ID
     * @return 同步任务 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 存在运行中的任务时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeSyncTaskVO triggerSync(AiKnowledgeSyncTriggerRequest request, Long operatorId) {
        String sourceType = request.getSourceType();

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
        executeSyncTask(task, request.getSourceId());
        return aiModelConvert.toKnowledgeSyncTaskVO(task);
    }

    /**
     * 重试失败的同步任务。
     *
     * <p>仅允许重试状态为 FAILED 且重试次数未超过上限的任务。
     *
     * @param taskId     任务 ID
     * @param operatorId 操作人 ID
     * @throws com.cybzacg.blogbackend.exception.BusinessException 任务不存在、状态不允许或重试次数超限时抛出
     */
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
        executeSyncTask(task, null);
    }

    /**
     * 分页查询同步任务列表。
     *
     * @param query 分页查询参数，支持按来源类型和状态过滤
     * @return 分页结果
     */
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

    /**
     * 获取指定同步任务的详情。
     *
     * @param taskId 任务 ID
     * @return 同步任务 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 任务不存在时抛出
     */
    @Override
    public AiKnowledgeSyncTaskVO getTask(Long taskId) {
        AiKnowledgeSyncTask task = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeSyncTaskRepository.getById(taskId),
                ResultErrorCode.AI_KNOWLEDGE_SYNC_TASK_NOT_FOUND);
        return aiModelConvert.toKnowledgeSyncTaskVO(task);
    }

    /**
     * 同步执行知识任务，完成条目抽取、分块和向量索引重建。
     */
    private void executeSyncTask(AiKnowledgeSyncTask task, Long sourceId) {
        task.setStatus(AiKnowledgeSyncTaskStatusEnum.RUNNING.getValue());
        task.setStartedAt(LocalDateTime.now());
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setSkipCount(0);
        task.setErrorMessage(null);
        aiKnowledgeSyncTaskRepository.updateById(task);

        int total = 0;
        int success = 0;
        int fail = 0;
        int skip = 0;
        String errorSummary = null;
        try {
            List<AiKnowledgeEntry> entries = loadEntriesForTask(task, sourceId);
            total = entries.size();
            // 逐条执行：upsert 条目 → 重建分块 → 更新状态
            for (AiKnowledgeEntry sourceEntry : entries) {
                try {
                    AiKnowledgeEntry persisted = upsertEntry(sourceEntry);
                    int chunkCount = aiKnowledgeChunkService.rebuildChunks(persisted);
                    persisted.setChunkCount(chunkCount);
                    persisted.setStatus(AiKnowledgeEntryStatusEnum.ACTIVE.getValue());
                    persisted.setSyncedAt(LocalDateTime.now());
                    aiKnowledgeEntryRepository.updateById(persisted);
                    success++;
                } catch (Exception ex) {
                    fail++;
                    errorSummary = truncateError(ex.getMessage());
                    log.warn("知识条目同步失败: taskId={}, sourceType={}, sourceId={}",
                            task.getId(), sourceEntry.getSourceType(), sourceEntry.getSourceId(), ex);
                }
            }
            // 全部成功标记 COMPLETED，部分失败标记 FAILED
            task.setStatus(fail > 0 ? AiKnowledgeSyncTaskStatusEnum.FAILED.getValue()
                    : AiKnowledgeSyncTaskStatusEnum.COMPLETED.getValue());
        } catch (Exception ex) {
            fail = Math.max(1, fail);
            errorSummary = truncateError(ex.getMessage());
            task.setStatus(AiKnowledgeSyncTaskStatusEnum.FAILED.getValue());
            log.warn("知识同步任务执行失败: taskId={}", task.getId(), ex);
        }
        task.setTotalCount(total);
        task.setSuccessCount(success);
        task.setFailCount(fail);
        task.setSkipCount(skip);
        task.setErrorMessage(errorSummary);
        task.setCompletedAt(LocalDateTime.now());
        aiKnowledgeSyncTaskRepository.updateById(task);
    }

    /**
     * 根据任务类型加载需要同步的知识条目列表。
     *
     * <p>支持三种任务类型：
     * <ul>
     *   <li>single_entry：仅抽取指定 sourceId 的单条条目</li>
     *   <li>incremental：仅抽取状态为待同步的候选条目</li>
     *   <li>full（默认）：全量抽取该来源类型的所有条目</li>
     * </ul>
     */
    private List<AiKnowledgeEntry> loadEntriesForTask(AiKnowledgeSyncTask task, Long sourceId) {
        if (AiConstants.SYNC_TASK_TYPE_SINGLE.equals(task.getTaskType())) {
            ExceptionThrowerCore.throwBusinessIf(sourceId == null,
                    ResultErrorCode.ILLEGAL_ARGUMENT, "single_entry 任务必须指定 sourceId");
            AiKnowledgeEntry entry = aiKnowledgeSourceExtractor.extractOne(task.getSourceType(), sourceId);
            return entry == null ? List.of() : List.of(entry);
        }
        if (AiConstants.SYNC_TASK_TYPE_INCREMENTAL.equals(task.getTaskType())) {
            return aiKnowledgeEntryRepository.listSyncCandidates(task.getSourceType(), task.getTaskType(), 1000)
                    .stream()
                    .map(entry -> aiKnowledgeSourceExtractor.extractOne(entry.getSourceType(), entry.getSourceId()))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        return aiKnowledgeSourceExtractor.extractAll(task.getSourceType());
    }

    /**
     * 新增或更新知识条目。若来源已存在则合并字段并递增版本号，否则新建。
     */
    private AiKnowledgeEntry upsertEntry(AiKnowledgeEntry sourceEntry) {
        AiKnowledgeEntry existing = aiKnowledgeEntryRepository.findBySource(
                sourceEntry.getSourceType(), sourceEntry.getSourceId());
        if (existing == null) {
            sourceEntry.setVersion(1);
            sourceEntry.setChunkCount(0);
            aiKnowledgeEntryRepository.save(sourceEntry);
            return sourceEntry;
        }
        existing.setTitle(sourceEntry.getTitle());
        existing.setSummary(sourceEntry.getSummary());
        existing.setContentSnapshot(sourceEntry.getContentSnapshot());
        existing.setSourceUrl(sourceEntry.getSourceUrl());
        existing.setAuthorId(sourceEntry.getAuthorId());
        existing.setSourceUpdatedAt(sourceEntry.getSourceUpdatedAt());
        existing.setStatus(AiKnowledgeEntryStatusEnum.ACTIVE.getValue());
        existing.setVersion(existing.getVersion() == null ? 1 : existing.getVersion() + 1);
        aiKnowledgeEntryRepository.updateById(existing);
        return existing;
    }

    /** 截断错误信息，避免超长消息写入数据库。 */
    private String truncateError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
