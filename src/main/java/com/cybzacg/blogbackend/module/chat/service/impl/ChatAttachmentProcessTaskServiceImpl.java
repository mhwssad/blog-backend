package com.cybzacg.blogbackend.module.chat.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatAttachmentProcessTask;
import com.cybzacg.blogbackend.mapper.ChatAttachmentProcessTaskMapper;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentProcessTaskService;
import java.util.Date;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 聊天附件异步处理任务基础服务实现。
 */
@Service
public class ChatAttachmentProcessTaskServiceImpl
        extends ServiceImpl<ChatAttachmentProcessTaskMapper, ChatAttachmentProcessTask>
        implements ChatAttachmentProcessTaskService {
    @Override
    public ChatAttachmentProcessTask saveOrResetPendingTask(Long messageId,
                                                            String messageType,
                                                            String messageSnapshotJson,
                                                            String pushUserIdsJson,
                                                            Integer maxRetryCount,
                                                            Date nextRetryAt) {
        ChatAttachmentProcessTask existing = lambdaQuery()
                .eq(ChatAttachmentProcessTask::getMessageId, messageId)
                .last("limit 1")
                .one();
        if (existing == null) {
            ChatAttachmentProcessTask task = buildPendingTask(
                    new ChatAttachmentProcessTask(),
                    messageId,
                    messageType,
                    messageSnapshotJson,
                    pushUserIdsJson,
                    maxRetryCount,
                    nextRetryAt
            );
            try {
                save(task);
                return task;
            } catch (DuplicateKeyException ex) {
                existing = lambdaQuery()
                        .eq(ChatAttachmentProcessTask::getMessageId, messageId)
                        .last("limit 1")
                        .one();
            }
        }
        ChatAttachmentProcessTask task = buildPendingTask(
                existing,
                messageId,
                messageType,
                messageSnapshotJson,
                pushUserIdsJson,
                maxRetryCount,
                nextRetryAt
        );
        updateById(task);
        return task;
    }

    @Override
    public List<ChatAttachmentProcessTask> listDispatchableTasks(Date executeBefore, int limit) {
        return lambdaQuery()
                .eq(ChatAttachmentProcessTask::getTaskStatus, ChatConstants.ATTACHMENT_TASK_STATUS_PENDING)
                .le(ChatAttachmentProcessTask::getNextRetryAt, executeBefore)
                .orderByAsc(ChatAttachmentProcessTask::getNextRetryAt)
                .orderByAsc(ChatAttachmentProcessTask::getId)
                .last("limit " + Math.max(limit, 1))
                .list();
    }

    @Override
    public boolean claimTask(Long taskId, Date executeBefore, Date leaseExpireAt) {
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

    @Override
    public int resetExpiredTasks(Date now, String lastError) {
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

    @Override
    public boolean markSuccess(Long taskId, Date completedAt) {
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

    @Override
    public boolean markRetry(Long taskId, int retryCount, Date nextRetryAt, String lastError) {
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

    @Override
    public boolean markFailed(Long taskId, int retryCount, Date completedAt, String lastError) {
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
                                                       Date nextRetryAt) {
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
