package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ChatAttachmentProcessTask;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天附件异步处理任务 Repository。
 */
public interface ChatAttachmentProcessTaskRepository extends IService<ChatAttachmentProcessTask> {

    /**
     * 为消息创建或重置一个待执行的附件处理任务。
     *
     * @param messageId 消息 ID
     * @param messageType 消息类型
     * @param messageSnapshotJson 消息快照 JSON
     * @param pushUserIdsJson 推送用户列表 JSON
     * @param maxRetryCount 最大重试次数
     * @param nextRetryAt 首次执行时间
     * @return 持久化后的任务
     */
    ChatAttachmentProcessTask saveOrResetPendingTask(Long messageId,
                                                     String messageType,
                                                     String messageSnapshotJson,
                                                     String pushUserIdsJson,
                                                     Integer maxRetryCount,
                                                     LocalDateTime nextRetryAt);

    /**
     * 查询当前到期且可执行的任务列表。
     *
     * @param executeBefore 允许执行的截止时间
     * @param limit 批次大小
     * @return 任务列表
     */
    List<ChatAttachmentProcessTask> listDispatchableTasks(LocalDateTime executeBefore, int limit);

    /**
     * 尝试抢占待执行任务，避免多节点重复处理。
     *
     * @param taskId 任务 ID
     * @param executeBefore 允许执行的截止时间
     * @param leaseExpireAt 本次执行租约过期时间
     * @return 是否抢占成功
     */
    boolean claimTask(Long taskId, LocalDateTime executeBefore, LocalDateTime leaseExpireAt);

    /**
     * 将处理超时的任务恢复为待执行状态。
     *
     * @param now 当前时间
     * @param lastError 恢复原因
     * @return 恢复的任务数
     */
    int resetExpiredTasks(LocalDateTime now, String lastError);

    /**
     * 标记任务处理成功。
     *
     * @param taskId 任务 ID
     * @param completedAt 完成时间
     * @return 是否更新成功
     */
    boolean markSuccess(Long taskId, LocalDateTime completedAt);

    /**
     * 标记任务稍后重试。
     *
     * @param taskId 任务 ID
     * @param retryCount 最新重试次数
     * @param nextRetryAt 下次重试时间
     * @param lastError 最近一次错误信息
     * @return 是否更新成功
     */
    boolean markRetry(Long taskId, int retryCount, LocalDateTime nextRetryAt, String lastError);

    /**
     * 标记任务最终失败。
     *
     * @param taskId 任务 ID
     * @param retryCount 最新重试次数
     * @param completedAt 完成时间
     * @param lastError 最近一次错误信息
     * @return 是否更新成功
     */
    boolean markFailed(Long taskId, int retryCount, LocalDateTime completedAt, String lastError);
}
