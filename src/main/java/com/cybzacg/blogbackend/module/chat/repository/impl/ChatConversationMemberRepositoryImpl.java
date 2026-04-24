package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatConversationMember;
import com.cybzacg.blogbackend.mapper.ChatConversationMemberMapper;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.repository.ChatConversationMemberRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 聊天会话成员 Repository 实现。
 */
@Repository
public class ChatConversationMemberRepositoryImpl extends ServiceImpl<ChatConversationMemberMapper, ChatConversationMember>
        implements ChatConversationMemberRepository {

    @Override
    public ChatConversationMember findByConversationAndUser(Long conversationId, Long userId) {
        return getOne(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getUserId, userId)
                .orderByDesc(ChatConversationMember::getId)
                .last("limit 1"), false);
    }

    @Override
    public ChatConversationMember findOwnerByConversationId(Long conversationId) {
        return getOne(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getMemberRole, ChatConstants.MEMBER_ROLE_OWNER)
                .orderByDesc(ChatConversationMember::getId)
                .last("limit 1"), false);
    }

    @Override
    public List<ChatConversationMember> listActiveByConversationId(Long conversationId) {
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL));
    }

    @Override
    public List<ChatConversationMember> listActiveByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .in(ChatConversationMember::getConversationId, conversationIds)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL));
    }

    @Override
    public List<ChatConversationMember> listByConversationId(Long conversationId) {
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .eq(ChatConversationMember::getConversationId, conversationId));
    }

    @Override
    public List<ChatConversationMember> listByConversationIds(Collection<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ChatConversationMember>()
                .in(ChatConversationMember::getConversationId, conversationIds));
    }

    @Override
    public boolean removeAllActiveMembers(Long conversationId) {
        return lambdaUpdate()
                .eq(ChatConversationMember::getConversationId, conversationId)
                .eq(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_NORMAL)
                .set(ChatConversationMember::getStatus, ChatConstants.MEMBER_STATUS_REMOVED)
                .update();
    }

    @Override
    public boolean advanceDeliveredState(Long id, Long messageId, Date deliveredAt) {
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
