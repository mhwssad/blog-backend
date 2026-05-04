package com.cybzacg.blogbackend.module.chat.governance.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.chat.ChatUserMuteRecord;
import com.cybzacg.blogbackend.enums.chat.ChatMuteRecordStatusEnum;
import com.cybzacg.blogbackend.mapper.chat.ChatUserMuteRecordMapper;
import com.cybzacg.blogbackend.module.chat.governance.repository.ChatUserMuteRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ChatUserMuteRecord Repository 实现。
 */
@Repository
public class ChatUserMuteRecordRepositoryImpl
        extends ServiceImpl<ChatUserMuteRecordMapper, ChatUserMuteRecord>
        implements ChatUserMuteRecordRepository {

    @Override
    public List<ChatUserMuteRecord> findActiveByUserIdAndScope(Long userId, String scope) {
        return list(new LambdaQueryWrapper<ChatUserMuteRecord>()
                .eq(ChatUserMuteRecord::getUserId, userId)
                .eq(ChatUserMuteRecord::getScope, scope)
                .eq(ChatUserMuteRecord::getStatus, ChatMuteRecordStatusEnum.ACTIVE.getValue())
                .orderByDesc(ChatUserMuteRecord::getId));
    }

    @Override
    public List<ChatUserMuteRecord> findActiveByUserIdAndConversationId(Long userId, Long conversationId) {
        return list(new LambdaQueryWrapper<ChatUserMuteRecord>()
                .eq(ChatUserMuteRecord::getUserId, userId)
                .eq(ChatUserMuteRecord::getConversationId, conversationId)
                .eq(ChatUserMuteRecord::getStatus, ChatMuteRecordStatusEnum.ACTIVE.getValue())
                .orderByDesc(ChatUserMuteRecord::getId));
    }

    @Override
    public List<ChatUserMuteRecord> findAllActiveByUserId(Long userId) {
        return list(new LambdaQueryWrapper<ChatUserMuteRecord>()
                .eq(ChatUserMuteRecord::getUserId, userId)
                .eq(ChatUserMuteRecord::getStatus, ChatMuteRecordStatusEnum.ACTIVE.getValue())
                .orderByDesc(ChatUserMuteRecord::getId));
    }

    @Override
    public Page<ChatUserMuteRecord> pageByAdminConditions(Long userId, String scope, Integer status, Page<ChatUserMuteRecord> page) {
        LambdaQueryWrapper<ChatUserMuteRecord> wrapper = new LambdaQueryWrapper<ChatUserMuteRecord>()
                .eq(Optional.ofNullable(userId).isPresent(), ChatUserMuteRecord::getUserId, userId)
                .eq(Optional.ofNullable(scope).isPresent(), ChatUserMuteRecord::getScope, scope)
                .eq(Optional.ofNullable(status).isPresent(), ChatUserMuteRecord::getStatus, status)
                .orderByDesc(ChatUserMuteRecord::getId);
        return page(page, wrapper);
    }
}
