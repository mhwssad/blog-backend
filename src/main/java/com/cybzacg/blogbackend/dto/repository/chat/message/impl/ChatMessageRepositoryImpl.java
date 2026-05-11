package com.cybzacg.blogbackend.dto.repository.chat.message.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.dto.mapper.chat.ChatMessageMapper;
import com.cybzacg.blogbackend.dto.repository.chat.message.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.message.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.shared.model.data.ChatMessageHistoryItem;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 聊天消息 Repository 实现。
 */
@Repository
public class ChatMessageRepositoryImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countMessagePage(Long conversationId, Long userId, Long beforeMessageId) {
        return baseMapper.countMessagePage(conversationId, userId, beforeMessageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatMessageHistoryItem> selectMessagePage(Long conversationId, Long userId, Long beforeMessageId, Long offset, Long size) {
        return baseMapper.selectMessagePage(conversationId, userId, beforeMessageId, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatMessageHistoryItem selectVisibleMessageById(Long conversationId, Long userId, Long messageId) {
        return baseMapper.selectVisibleMessageById(conversationId, userId, messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatMessageHistoryItem> selectVisibleMessagesByIds(Long conversationId, Long userId, Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectVisibleMessagesByIds(conversationId, userId, messageIds.stream().toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countAdminMessagePage(Long conversationId, ChatAdminMessagePageQuery query) {
        return baseMapper.countAdminMessagePage(conversationId, query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatAdminMessageItem> selectAdminMessagePage(Long conversationId, ChatAdminMessagePageQuery query, Long offset, Long size) {
        return baseMapper.selectAdminMessagePage(conversationId, query, offset, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatAdminMessageItem> selectAdminMessagesByIds(Long conversationId, Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectAdminMessagesByIds(conversationId, messageIds.stream().toList());
    }

    /**
     * 根据发送者和客户端消息 ID 查找消息，按 ID 降序取最新一条，用于幂等去重。
     */
    @Override
    public ChatMessage findBySenderAndClientMessageId(Long senderId, String clientMessageId) {
        return getOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSenderId, senderId)
                .eq(ChatMessage::getClientMessageId, clientMessageId)
                .orderByDesc(ChatMessage::getId)
                .last("limit 1"), false);
    }

    @Override
    public ChatMessage findLatestBySenderAndConversation(Long senderId, Long conversationId) {
        return getOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSenderId, senderId)
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getId)
                .last("limit 1"), false);
    }
}
