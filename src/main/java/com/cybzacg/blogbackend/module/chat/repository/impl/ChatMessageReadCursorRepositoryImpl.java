package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ChatMessageReadCursor;
import com.cybzacg.blogbackend.mapper.ChatMessageReadCursorMapper;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageReadCursorRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * 聊天会话已读游标 Repository 实现。
 */
@Repository
public class ChatMessageReadCursorRepositoryImpl extends ServiceImpl<ChatMessageReadCursorMapper, ChatMessageReadCursor>
        implements ChatMessageReadCursorRepository {

    @Override
    public ChatMessageReadCursor findByConversationAndUser(Long conversationId, Long userId) {
        return getOne(new LambdaQueryWrapper<ChatMessageReadCursor>()
                .eq(ChatMessageReadCursor::getConversationId, conversationId)
                .eq(ChatMessageReadCursor::getUserId, userId)
                .orderByDesc(ChatMessageReadCursor::getId)
                .last("limit 1"), false);
    }

    @Override
    public boolean advanceDeliveredState(Long id, Long messageId, Date deliveredAt) {
        return lambdaUpdate()
                .eq(ChatMessageReadCursor::getId, id)
                .and(wrapper -> wrapper.isNull(ChatMessageReadCursor::getDeliveredMessageId)
                        .or()
                        .lt(ChatMessageReadCursor::getDeliveredMessageId, messageId))
                .set(ChatMessageReadCursor::getDeliveredMessageId, messageId)
                .set(ChatMessageReadCursor::getDeliveredAt, deliveredAt)
                .update();
    }
}
