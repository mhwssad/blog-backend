package com.cybzacg.blogbackend.dto.repository.chat.message.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.common.constant.ChatConstants;
import com.cybzacg.blogbackend.dto.domain.chat.ChatMessageRecipient;
import com.cybzacg.blogbackend.dto.mapper.chat.ChatMessageRecipientMapper;
import com.cybzacg.blogbackend.dto.repository.chat.message.ChatMessageRecipientRepository;
import com.cybzacg.blogbackend.module.chat.message.model.admin.ChatAdminMessageReceiptPageQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 聊天消息接收状态 Repository 实现。
 */
@Repository
public class ChatMessageRecipientRepositoryImpl extends ServiceImpl<ChatMessageRecipientMapper, ChatMessageRecipient>
        implements ChatMessageRecipientRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hideMessage(Long conversationId, Long recipientUserId, Long messageId) {
        return lambdaUpdate()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .set(ChatMessageRecipient::getVisibleStatus, ChatConstants.VISIBLE_STATUS_HIDDEN)
                .update();
    }

    /**
     * 将当前用户在该会话中 message_id <= 目标值且仍可见的记录全部标为已读，
     * delivered_at 和 read_at 统一使用 readAt 以保证时序一致。
     */
    @Override
    public boolean markReadUpTo(Long conversationId, Long recipientUserId, Long messageId, LocalDateTime readAt) {
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

    /**
     * 仅将当前状态为 PENDING 的记录更新为 DELIVERED，避免覆盖已读状态。
     */
    @Override
    public boolean markDelivered(Long conversationId, Long recipientUserId, Long messageId, LocalDateTime deliveredAt) {
        return lambdaUpdate()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .eq(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_PENDING)
                .set(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_DELIVERED)
                .set(ChatMessageRecipient::getDeliveredAt, deliveredAt)
                .update();
    }

    /**
     * 批量标记已投递，仅更新状态低于 DELIVERED 的记录，防止降级已读状态。
     */
    @Override
    public boolean batchMarkDelivered(Long conversationId, Long recipientUserId, Collection<Long> messageIds, LocalDateTime deliveredAt) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public long countUnread(Long conversationId, Long recipientUserId) {
        return count(new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getConversationId, conversationId)
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getVisibleStatus, ChatConstants.VISIBLE_STATUS_VISIBLE)
                .lt(ChatMessageRecipient::getDeliveryStatus, ChatConstants.DELIVERY_STATUS_READ));
    }

    /**
     * 根据用户和消息查找可见的投递记录，按 ID 降序取最新一条。
     */
    @Override
    public ChatMessageRecipient findVisibleByUserAndMessage(Long recipientUserId, Long messageId) {
        return getOne(new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getRecipientUserId, recipientUserId)
                .eq(ChatMessageRecipient::getMessageId, messageId)
                .eq(ChatMessageRecipient::getVisibleStatus, ChatConstants.VISIBLE_STATUS_VISIBLE)
                .orderByDesc(ChatMessageRecipient::getId)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatMessageRecipient> listByMessageId(Long messageId) {
        return list(new LambdaQueryWrapper<ChatMessageRecipient>()
                .eq(ChatMessageRecipient::getMessageId, messageId));
    }

    /**
     * 管理后台投递记录分页查询，对空查询参数做防御性默认值处理，
     * 并根据前端传入的筛选条件动态拼接 WHERE 子句。
     */
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
