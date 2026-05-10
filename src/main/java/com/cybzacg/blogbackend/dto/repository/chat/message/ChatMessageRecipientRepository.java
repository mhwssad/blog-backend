package com.cybzacg.blogbackend.dto.repository.chat.message;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.chat.ChatMessageRecipient;
import com.cybzacg.blogbackend.module.chat.message.model.admin.ChatAdminMessageReceiptPageQuery;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 聊天消息接收状态 Repository。<p>
 * 封装消息接收状态（投递、已读、隐藏）的持久化操作，支持单条和批量状态变更及管理后台投递记录分页。
 */
public interface ChatMessageRecipientRepository extends IService<ChatMessageRecipient> {

    /**
     * 将指定消息对某个接收者标记为隐藏。
     */
    boolean hideMessage(Long conversationId, Long recipientUserId, Long messageId);

    /**
     * 将指定接收者在某会话中不超过目标消息 ID 的全部可见消息标记为已读。
     */
    boolean markReadUpTo(Long conversationId, Long recipientUserId, Long messageId, LocalDateTime readAt);

    /**
     * 将单条消息对某个接收者的状态从待投递更新为已投递。
     */
    boolean markDelivered(Long conversationId, Long recipientUserId, Long messageId, LocalDateTime deliveredAt);

    /**
     * 将多条消息批量标记为已投递，仅更新当前状态低于已投递的记录。
     */
    boolean batchMarkDelivered(Long conversationId, Long recipientUserId, Collection<Long> messageIds, LocalDateTime deliveredAt);

    /**
     * 统计指定接收者在某会话中的未读消息数。
     */
    long countUnread(Long conversationId, Long recipientUserId);

    /**
     * 查找接收者在指定消息上的可见投递记录。
     */
    ChatMessageRecipient findVisibleByUserAndMessage(Long recipientUserId, Long messageId);

    /**
     * 查询指定消息的全部投递记录。
     */
    List<ChatMessageRecipient> listByMessageId(Long messageId);

    /**
     * 管理后台分页查询指定消息的投递记录。
     */
    Page<ChatMessageRecipient> pageAdminReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query);
}
