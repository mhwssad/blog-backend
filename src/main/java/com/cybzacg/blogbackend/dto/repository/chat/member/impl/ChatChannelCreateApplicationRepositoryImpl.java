package com.cybzacg.blogbackend.dto.repository.chat.member.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.chat.ChatChannelCreateApplication;
import com.cybzacg.blogbackend.dto.mapper.chat.ChatChannelCreateApplicationMapper;
import com.cybzacg.blogbackend.dto.repository.chat.member.ChatChannelCreateApplicationRepository;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatChannelApplicationAdminPageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

/**
 * 频道创建申请 Repository 实现。
 */
@Repository
public class ChatChannelCreateApplicationRepositoryImpl
        extends ServiceImpl<ChatChannelCreateApplicationMapper, ChatChannelCreateApplication>
        implements ChatChannelCreateApplicationRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatChannelCreateApplication findLatestByApplicantUserId(Long applicantUserId) {
        return getOne(new LambdaQueryWrapper<ChatChannelCreateApplication>()
                .eq(ChatChannelCreateApplication::getApplicantUserId, applicantUserId)
                .orderByDesc(ChatChannelCreateApplication::getSubmittedAt)
                .orderByDesc(ChatChannelCreateApplication::getId)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ChatChannelCreateApplication> pageByApplicantUserId(Long applicantUserId, long current, long size) {
        return page(new Page<>(current, size), new LambdaQueryWrapper<ChatChannelCreateApplication>()
                .eq(ChatChannelCreateApplication::getApplicantUserId, applicantUserId)
                .orderByDesc(ChatChannelCreateApplication::getSubmittedAt)
                .orderByDesc(ChatChannelCreateApplication::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<ChatChannelCreateApplication> pageByAdminConditions(ChatChannelApplicationAdminPageQuery query) {
        LambdaQueryWrapper<ChatChannelCreateApplication> wrapper = new LambdaQueryWrapper<ChatChannelCreateApplication>()
                .eq(query.getApplicantUserId() != null, ChatChannelCreateApplication::getApplicantUserId, query.getApplicantUserId())
                .eq(query.getApplyStatus() != null, ChatChannelCreateApplication::getApplyStatus, query.getApplyStatus())
                .like(StrUtils.hasText(query.getKeyword()), ChatChannelCreateApplication::getDesiredName, query.getKeyword())
                .orderByDesc(ChatChannelCreateApplication::getSubmittedAt)
                .orderByDesc(ChatChannelCreateApplication::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }
}
