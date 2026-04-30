package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.chat.ChatGroupInviteLink;
import com.cybzacg.blogbackend.mapper.chat.ChatGroupInviteLinkMapper;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupInviteLinkPageQuery;
import com.cybzacg.blogbackend.module.chat.repository.ChatGroupInviteLinkRepository;
import org.springframework.stereotype.Repository;

/**
 * 群聊邀请链接 Repository 实现。
 */
@Repository
public class ChatGroupInviteLinkRepositoryImpl
        extends ServiceImpl<ChatGroupInviteLinkMapper, ChatGroupInviteLink>
        implements ChatGroupInviteLinkRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatGroupInviteLink findByToken(String inviteToken) {
        return getOne(new LambdaQueryWrapper<ChatGroupInviteLink>()
                .eq(ChatGroupInviteLink::getInviteToken, inviteToken)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ChatGroupInviteLink> pageByConversationId(Long conversationId, ChatGroupInviteLinkPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), new LambdaQueryWrapper<ChatGroupInviteLink>()
                .eq(ChatGroupInviteLink::getConversationId, conversationId)
                .eq(query.getStatus() != null, ChatGroupInviteLink::getStatus, query.getStatus())
                .orderByDesc(ChatGroupInviteLink::getCreatedAt)
                .orderByDesc(ChatGroupInviteLink::getId));
    }
}
