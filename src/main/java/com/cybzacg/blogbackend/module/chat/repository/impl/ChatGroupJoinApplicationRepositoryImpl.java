package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatGroupJoinApplication;
import com.cybzacg.blogbackend.mapper.ChatGroupJoinApplicationMapper;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupJoinApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.repository.ChatGroupJoinApplicationRepository;
import org.springframework.stereotype.Repository;

/**
 * 群聊入群申请 Repository 实现。
 */
@Repository
public class ChatGroupJoinApplicationRepositoryImpl
        extends ServiceImpl<ChatGroupJoinApplicationMapper, ChatGroupJoinApplication>
        implements ChatGroupJoinApplicationRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatGroupJoinApplication findLatestByConversationAndApplicant(Long conversationId, Long applicantUserId) {
        return getOne(new LambdaQueryWrapper<ChatGroupJoinApplication>()
                .eq(ChatGroupJoinApplication::getConversationId, conversationId)
                .eq(ChatGroupJoinApplication::getApplicantUserId, applicantUserId)
                .orderByDesc(ChatGroupJoinApplication::getSubmittedAt)
                .orderByDesc(ChatGroupJoinApplication::getId)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ChatGroupJoinApplication> pageByApplicantUserId(Long applicantUserId, ChatGroupJoinApplicationPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), baseWrapper(query)
                .eq(ChatGroupJoinApplication::getApplicantUserId, applicantUserId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ChatGroupJoinApplication> pageByConversationId(Long conversationId, ChatGroupJoinApplicationPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), baseWrapper(query)
                .eq(ChatGroupJoinApplication::getConversationId, conversationId));
    }

    private LambdaQueryWrapper<ChatGroupJoinApplication> baseWrapper(ChatGroupJoinApplicationPageQuery query) {
        return new LambdaQueryWrapper<ChatGroupJoinApplication>()
                .eq(query.getApplyStatus() != null, ChatGroupJoinApplication::getApplyStatus, query.getApplyStatus())
                .orderByDesc(ChatGroupJoinApplication::getSubmittedAt)
                .orderByDesc(ChatGroupJoinApplication::getId);
    }
}
