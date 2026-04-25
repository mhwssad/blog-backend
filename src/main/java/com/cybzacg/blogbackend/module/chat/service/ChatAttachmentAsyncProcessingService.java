package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;

import java.util.Collection;

/**
 * 聊天附件异步处理服务。
 */
public interface ChatAttachmentAsyncProcessingService {
    /**
     * 在消息事务提交后调度图片缩略图、语音转码和波形处理。
     *
     * @param messageId       消息 ID
     * @param messageSnapshot 当前发送链路返回给前端的消息快照
     * @param pushUserIds     需要接收更新事件的用户 ID 列表
     */
    void scheduleAfterCommit(Long messageId, ChatMessageVO messageSnapshot, Collection<Long> pushUserIds);

    /**
     * 派发当前已到执行时间的持久化媒体处理任务。
     */
    void dispatchDueTasks();

    /**
     * 回收租约过期的处理中任务，便于后续重新派发。
     *
     * @return 恢复的任务数
     */
    int recoverExpiredTasks();
}
