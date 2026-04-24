package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatMessageRecipient;
import com.cybzacg.blogbackend.mapper.ChatMessageRecipientMapper;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessageReceiptPageQuery;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageRecipientRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 聊天消息接收状态 Repository 实现。
 */
@Repository
public class ChatMessageRecipientRepositoryImpl extends ServiceImpl<ChatMessageRecipientMapper, ChatMessageRecipient>
        implements ChatMessageRecipientRepository {

    @Override
    public boolean hideMessage(Long conversationId, Long recipientUserId, Long messageId) {
        return lambdaUpdate()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .set(ChatMessageRecipient::getVisibleStatus, ChatConstants.VISIBLE_STATUS_HIDDEN)
                .update();
    }

    @Override
    public boolean markReadUpTo(Long conversationId, Long recipientUserId, Long messageId, Date readAt) {
        return lambdaUpdate()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getVisibleStatus, ChatConstants.VISIBLE_STATUS_VISIBLE)
                .le(ChatMessageRecipient::getMessageId, messageId)
                .set(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_READ)
                .set(ChatMessageRecipient::getDeliveredAt, readAt)
                .set(ChatMessageRecipient::getReadAt, readAt)
                .update();
    }

    @Override
    public boolean markDelivered(Long conversationId, Long recipientUserId, Long messageId, Date deliveredAt) {
        return lambdaUpdate()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .eq(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_PENDING)
                .set(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_DELIVERED)
                .set(ChatMessageRecipient::getDeliveredAt, deliveredAt)
                .update();
    }

    @Override
    public boolean batchMarkDelivered(Long conversationId, Long recipientUserId, Collection<Long> messageIds, Date deliveredAt) {
        if (messageIds == null || messageIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .in(ChatMessageRecipient::getMessageId, messageIds)
                .lt(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_DELIVERED)
                .set(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_DELIVERED)
                .set(ChatMessageRecipient::getDeliveredAt, deliveredAt)
                .update();
    }

    @Override
    public long countUnread(Long conversationId, Long recipientUserId) {
        return count(new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getVisibleStatus, ChatConstants.VISIBLE_STATUS_VISIBLE)
                .lt(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_READ));
    }

    @Override
    public ChatMessageRecipient findVisibleByUserAndMessage(Long recipientUserId, Long messageId) {
        return getOne(new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .eq(ChatMessageRecipient::getVisibleStatus, ChatConstants.VISIBLE_STATUS_VISIBLE)
                .orderByDesc(ChatMessageRecipient::getId)
                .last("limit 1"), false);
    }

    @Override
    public List<ChatMessageRecipient> listByMessageId(Long messageId) {
        return list(new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getMessageId, messageId));
    }

    @Override
    public Page<ChatMessageRecipient> pageAdminReceipts(Long conversationId, Long messageId, ChatAdminMessageReceiptPageQuery query) {
        ChatAdminMessageReceiptPageQuery safeQuery = query == null ? new ChatAdminMessageReceiptPageQuery() : query;
        long current = safeQuery.getCurrent() == null ? 1L : safeQuery.getCurrent();
        long size = safeQuery.getSize() == null ? 20L : safeQuery.getSize();
        return page(new Page<>(current, size), new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .eq(safeQuery.getRecipientUserId() != null, ChatMessageRecipient::getRecipientUserId, safeQuery.getRecipientUserId())
                .eq(safeQuery.getDeliveryStatus() != null, ChatMessageRecipient::getDeliveryStatus, safeQuery.getDeliveryStatus())
                .eq(safeQuery.getVisibleStatus() != null, ChatMessageRecipient::getVisibleStatus, safeQuery.getVisibleStatus())
                .orderByDesc(ChatMessageRecipient::getId));
    }
}
