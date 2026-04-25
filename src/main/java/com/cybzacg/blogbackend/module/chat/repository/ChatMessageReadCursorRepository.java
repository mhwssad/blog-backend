package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ChatMessageReadCursor;

import java.time.LocalDateTime;

/**
 * 聊天会话已读游标 Repository。<p>
 * 封装每个用户在每个会话中的已读/已投递游标持久化操作。
 */
public interface ChatMessageReadCursorRepository extends IService<ChatMessageReadCursor> {

    /** 根据会话 ID 和用户 ID 查找已读游标记录。 */
    ChatMessageReadCursor findByConversationAndUser(Long conversationId, Long userId);

    /**
     * 单调递增更新游标的已投递游标（CAS 语义）。
     * 仅当当前 deliveredMessageId 为空或小于目标值时才更新。
     *
     * @param id 游标记录 ID
     * @param messageId 目标已投递消息 ID
     * @param deliveredAt 投递时间
     * @return 是否更新成功
     */
    boolean advanceDeliveredState(Long id, Long messageId, LocalDateTime deliveredAt);
}
