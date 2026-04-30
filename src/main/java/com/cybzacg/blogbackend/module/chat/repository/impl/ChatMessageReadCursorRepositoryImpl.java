package com.cybzacg.blogbackend.module.chat.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.chat.ChatMessageReadCursor;
import com.cybzacg.blogbackend.mapper.chat.ChatMessageReadCursorMapper;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageReadCursorRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 聊天会话已读游标 Repository 实现。
 */
@Repository
public class ChatMessageReadCursorRepositoryImpl extends ServiceImpl<ChatMessageReadCursorMapper, ChatMessageReadCursor>
        implements ChatMessageReadCursorRepository {

    /**
     * 根据会话和用户查找游标记录，按 ID 降序取最新一条。
     */
    @Override
    public ChatMessageReadCursor findByConversationAndUser(Long conversationId, Long userId) {
        return getOne(new LambdaQueryWrapper<ChatMessageReadCursor>()
                .eq(ChatMessageReadCursor::getConversationId, conversationId)
                .eq(ChatMessageReadCursor::getUserId, userId)
                .orderByDesc(ChatMessageReadCursor::getId)
                .last("limit 1"), false);
    }

    /**
     * CAS 式推进已投递游标，仅在当前值为空或小于目标值时更新，防止并发回退。
     */
    @Override
    public boolean advanceDeliveredState(Long id, Long messageId, LocalDateTime deliveredAt) {
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
