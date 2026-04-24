package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ChatMessageRecipient;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptPageQuery;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 聊天消息接收状态 Repository。
 */
public interface ChatMessageRecipientRepository extends IService<ChatMessageRecipient> {
    boolean hideMessage(Long conversationId, Long recipientUserId, Long messageId);

    boolean markReadUpTo(Long conversationId, Long recipientUserId, Long messageId, Date readAt);

    boolean markDelivered(Long conversationId, Long recipientUserId, Long messageId, Date deliveredAt);

    boolean batchMarkDelivered(Long conversationId, Long recipientUserId, Collection<Long> messageIds, Date deliveredAt);

    long countUnread(Long conversationId, Long recipientUserId);

    ChatMessageRecipient findVisibleByUserAndMessage(Long recipientUserId, Long messageId);

    List<ChatMessageRecipient> listByMessageId(Long messageId);

    Page<ChatMessageRecipient> pageAdminReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query);
}
