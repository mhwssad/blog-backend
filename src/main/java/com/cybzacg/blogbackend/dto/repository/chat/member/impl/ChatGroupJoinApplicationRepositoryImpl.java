package com.cybzacg.blogbackend.module.chat.member.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.chat.ChatGroupJoinApplication;
import com.cybzacg.blogbackend.dto.mapper.chat.ChatGroupJoinApplicationMapper;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupJoinApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.member.repository.ChatGroupJoinApplicationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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
        return page(new Page<>(query.getCurrent(), query.getSize()), baseWrapper(query.getApplyStatus(), null)
                .eq(ChatGroupJoinApplication::getApplicantUserId, applicantUserId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ChatGroupJoinApplication> pageByConversationId(Long conversationId, ChatGroupJoinApplicationPageQuery query) {
        return page(new Page<>(query.getCurrent(), query.getSize()), baseWrapper(query.getApplyStatus(), null)
                .eq(ChatGroupJoinApplication::getConversationId, conversationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ChatGroupJoinApplication> pageByAdminConditions(ChatGroupJoinApplicationAdminPageQuery query) {
        LambdaQueryWrapper<ChatGroupJoinApplication> wrapper = baseWrapper(query.getApplyStatus(), query.getKeyword())
                .eq(query.getConversationId() != null, ChatGroupJoinApplication::getConversationId, query.getConversationId())
                .eq(query.getApplicantUserId() != null, ChatGroupJoinApplication::getApplicantUserId, query.getApplicantUserId());
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    private LambdaQueryWrapper<ChatGroupJoinApplication> baseWrapper(Integer applyStatus, String keyword) {
        LambdaQueryWrapper<ChatGroupJoinApplication> wrapper = new LambdaQueryWrapper<ChatGroupJoinApplication>()
                .eq(applyStatus != null, ChatGroupJoinApplication::getApplyStatus, applyStatus)
                .orderByDesc(ChatGroupJoinApplication::getSubmittedAt)
                .orderByDesc(ChatGroupJoinApplication::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(ChatGroupJoinApplication::getApplyMessage, keyword)
                    .or()
                    .like(ChatGroupJoinApplication::getReviewComment, keyword));
        }
        return wrapper;
    }
}
