package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.chat.ChatConversationMember;
import com.cybzacg.blogbackend.mapper.chat.ChatConversationMemberMapper;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationMemberRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 聊天会话成员 Repository 实现。
 */
@Repository
public class ChatConversationMemberRepositoryImpl extends ServiceImpl<ChatConversationMemberMapper, ChatConversationMember>
        implements ChatConversationMemberRepository {

    /**
     * 根据会话和用户查找成员记录，按 ID 降序取最新一条。
     */
    @Override
    public ChatConversationMember findByConversationAndUser(Long conversationId, Long userId) {
        return getOne(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getUserId, userId)
                .orderByDesc(ChatConversationMember::getId)
                .last("limit 1"), false);
    }

    /**
     * 查找会话群主，按 ID 降序取最新一条。
     */
    @Override
    public ChatConversationMember findOwnerByConversationId(Long conversationId) {
        return getOne(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getMemberRole, ChatConstants.MEMBER_ROLE_OWNER)
                .orderByDesc(ChatConversationMember::getId)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatConversationMember> listActiveByConversationId(Long conversationId) {
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatConversationMember> listActiveByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .in(ChatConversationMember::getConversationId, conversationIds)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatConversationMember> listByConversationId(Long conversationId) {
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatConversationMember> listByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .in(ChatConversationMember::getConversationId, conversationIds));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAllActiveMembers(Long conversationId) {
        return lambdaUpdate()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL)
                .set(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_REMOVED)
                .update();
    }

    /**
     * 统计指定会话中状态为正常的成员数量。
     */
    @Override
    public long countActiveByConversationId(Long conversationId) {
        return count(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL));
    }

    /**
     * CAS 式推进已投递游标，仅在当前值为空或小于目标值时更新，避免并发回退。
     */
    @Override
    public boolean advanceDeliveredState(Long id, Long messageId, LocalDateTime deliveredAt) {
        return lambdaUpdate()
                .eq(ChatConversationMember::getId, id)
                .and(wrapper -> wrapper.isNull(ChatConversationMember::getLastDeliveredMessageId)
                        .or()
                        .lt(ChatConversationMember::getLastDeliveredMessageId, messageId))
                .set(ChatConversationMember::getLastDeliveredMessageId, messageId)
                .set(ChatConversationMember::getLastDeliveredAt, deliveredAt)
                .update();
    }
}
