package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;

import java.util.Collection;
import java.util.List;

/**
 * 聊天消息 Repository。
 */
public interface ChatMessageRepository extends IService<ChatMessage> {
    Long countMessagePage(Long conversationId, Long userId, Long beforeMessageId);

    List<ChatMessageHistoryItem> selectMessagePage(Long conversationId, Long userId, Long beforeMessageId, Long offset, Long size);

    ChatMessageHistoryItem selectVisibleMessageById(Long conversationId, Long userId, Long messageId);

    List<ChatMessageHistoryItem> selectVisibleMessagesByIds(Long conversationId, Long userId, Collection<Long> messageIds);

    Long countAdminMessagePage(Long conversationId, ChatAdminMessagePageQuery query);

    List<ChatAdminMessageItem> selectAdminMessagePage(Long conversationId, ChatAdminMessagePageQuery query, Long offset, Long size);

    List<ChatAdminMessageItem> selectAdminMessagesByIds(Long conversationId, Collection<Long> messageIds);

    ChatMessage findBySenderAndClientMessageId(Long senderId, String clientMessageId);
}
