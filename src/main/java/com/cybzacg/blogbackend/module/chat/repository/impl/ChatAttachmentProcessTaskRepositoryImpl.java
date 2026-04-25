package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatAttachmentProcessTask;
import com.cybzacg.blogbackend.mapper.ChatAttachmentProcessTaskMapper;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.repository.ChatAttachmentProcessTaskRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天附件异步处理任务 Repository 实现。
 */
@Repository
public class ChatAttachmentProcessTaskRepositoryImpl
        extends ServiceImpl<ChatAttachmentProcessTaskMapper, ChatAttachmentProcessTask>
        implements ChatAttachmentProcessTaskRepository {

    /**
     * 为消息创建或重置待执行任务。<p>
     * 先查询是否已有记录：若无则新建；若已存在则重置为待执行状态。
     * 新建时通过捕获 DuplicateKeyException 处理并发场景下的唯一键冲突，
     * 冲突后重新查询并走重置逻辑。
     */
    @Override
    public ChatAttachmentProcessTask saveOrResetPendingTask(Long messageId,
                                                            String messageType,
                                                            String messageSnapshotJson,
                                                            String pushUserIdsJson,
                                                            Integer maxRetryCount,
                                                            LocalDateTime nextRetryAt) {
        ChatAttachmentProcessTask existing = lambdaQuery()
                .eq(ChatAttachmentProcessTask::getMessageId, messageId)
                .last("limit 1")
                .one();
        if (existing == null) {
            ChatAttachmentProcessTask task = buildPendingTask(
                    new ChatAttachmentProcessTask(),
                    messageId, messageType, messageSnapshotJson,
                    pushUserIdsJson, maxRetryCount, nextRetryAt
            );
            try {
                save(task);
                return task;
            } catch (DuplicateKeyException ex) {
                // 并发写入导致唯一键冲突，重新查询已有记录并走重置逻辑
                existing = lambdaQuery()
                        .eq(ChatAttachmentProcessTask::getMessageId, messageId)
                        .last("limit 1")
                        .one();
            }
        }
        ChatAttachmentProcessTask task = buildPendingTask(
                existing, messageId, messageType, messageSnapshotJson,
                pushUserIdsJson, maxRetryCount, nextRetryAt
        );
        updateById(task);
        return task;
    }

    /**
     * 查询到期可执行的任务，按 nextRetryAt 和 ID 升序排列以保证调度公平性。
     */
    @Override
    public List<ChatAttachmentProcessTask> listDispatchableTasks(LocalDateTime executeBefore, int limit) {
        return lambdaQuery()
                .eq(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PENDING)
                .le(ChatAttachmentProcessTask::getNextRetryAt, executeBefore)
                .orderByAsc(ChatAttachmentProcessTask::getNextRetryAt)
                .orderByAsc(ChatAttachmentProcessTask::getId)
                .last("limit " + Math.max(limit, 1))
                .list();
    }

    /**
     * 抢占式认领任务，通过 status + nextRetryAt 条件保证同一任务不会被多个节点重复执行。
     */
    @Override
    public boolean claimTask(Long taskId, LocalDateTime executeBefore, LocalDateTime leaseExpireAt) {
        return lambdaUpdate()
                .eq(ChatAttachmentProcessTask::getId, taskId)
                .eq(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PENDING)
                .le(ChatAttachmentProcessTask::getNextRetryAt, executeBefore)
                .set(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PROCESSING)
                .set(ChatAttachmentProcessTask::getStartedAt, executeBefore)
                .set(ChatAttachmentProcessTask::getLeaseExpireAt, leaseExpireAt)
                .set(ChatAttachmentProcessTask::getCompletedAt, null)
                .set(ChatAttachmentProcessTask::getLastError, null)
                .update();
    }

    /**
     * 将租约过期的 PROCESSING 任务恢复为 PENDING，使其可被重新调度。
     */
    @Override
    public int resetExpiredTasks(LocalDateTime now, String lastError) {
        return baseMapper.update(
                null,
                Wrappers.<ChatAttachmentProcessTask>lambdaUpdate()
                        .eq(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PROCESSING)
                        .isNotNull(ChatAttachmentProcessTask::getLeaseExpireAt)
                        .le(ChatAttachmentProcessTask::getLeaseExpireAt, now)
                        .set(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PENDING)
                        .set(ChatAttachmentProcessTask::getNextRetryAt, now)
                        .set(ChatAttachmentProcessTask::getLeaseExpireAt, null)
                        .set(ChatAttachmentProcessTask::getStartedAt, null)
                        .set(ChatAttachmentProcessTask::getLastError, lastError));
    }

    /**
     * 标记任务处理成功，清除租约和重试相关字段。
     */
    @Override
    public boolean markSuccess(Long taskId, LocalDateTime completedAt) {
        return lambdaUpdate()
                .eq(ChatAttachmentProcessTask::getId, taskId)
                .eq(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PROCESSING)
                .set(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_SUCCESS)
                .set(ChatAttachmentProcessTask::getLeaseExpireAt, null)
                .set(ChatAttachmentProcessTask::getCompletedAt, completedAt)
                .set(ChatAttachmentProcessTask::getNextRetryAt, null)
                .set(ChatAttachmentProcessTask::getLastError, null)
                .update();
    }

    /**
     * 标记任务稍后重试，将状态回退为 PENDING 并重置执行上下文字段。
     */
    @Override
    public boolean markRetry(Long taskId, int retryCount, LocalDateTime nextRetryAt, String lastError) {
        return lambdaUpdate()
                .eq(ChatAttachmentProcessTask::getId, taskId)
                .eq(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PROCESSING)
                .set(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PENDING)
                .set(ChatAttachmentProcessTask::getRetryCount, retryCount)
                .set(ChatAttachmentProcessTask::getNextRetryAt, nextRetryAt)
                .set(ChatAttachmentProcessTask::getLeaseExpireAt, null)
                .set(ChatAttachmentProcessTask::getStartedAt, null)
                .set(ChatAttachmentProcessTask::getCompletedAt, null)
                .set(ChatAttachmentProcessTask::getLastError, lastError)
                .update();
    }

    /**
     * 标记任务最终失败，不再重试，清除调度相关字段。
     */
    @Override
    public boolean markFailed(Long taskId, int retryCount, LocalDateTime completedAt, String lastError) {
        return lambdaUpdate()
                .eq(ChatAttachmentProcessTask::getId, taskId)
                .eq(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PROCESSING)
                .set(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_FAILED)
                .set(ChatAttachmentProcessTask::getRetryCount, retryCount)
                .set(ChatAttachmentProcessTask::getNextRetryAt, null)
                .set(ChatAttachmentProcessTask::getLeaseExpireAt, null)
                .set(ChatAttachmentProcessTask::getCompletedAt, completedAt)
                .set(ChatAttachmentProcessTask::getLastError, lastError)
                .update();
    }

    private ChatAttachmentProcessTask buildPendingTask(ChatAttachmentProcessTask task,
                                                       Long messageId,
                                                       String messageType,
                                                       String messageSnapshotJson,
                                                       String pushUserIdsJson,
                                                       Integer maxRetryCount,
                                                       LocalDateTime nextRetryAt) {
        task.setMessageId(messageId);
        task.setMessageType(messageType);
        task.setTaskStatus(ChatConstants.ATTACHMENT_TASK_STATUS_PENDING);
        task.setRetryCount(0);
        task.setMaxRetryCount(maxRetryCount);
        task.setNextRetryAt(nextRetryAt);
        task.setLeaseExpireAt(null);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        task.setLastError(null);
        task.setMessageSnapshotJson(messageSnapshotJson);
        task.setPushUserIdsJson(pushUserIdsJson);
        return task;
    }
}
