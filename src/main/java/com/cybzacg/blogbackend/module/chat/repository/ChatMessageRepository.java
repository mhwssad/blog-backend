package com.cybzacg.blogbackend.module.chat.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.module.chat.model.admin.ChatAdminMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.model.data.ChatAdminMessageItem;
import com.cybzacg.blogbackend.module.chat.model.data.ChatMessageHistoryItem;

import java.util.Collection;
import java.util.List;

/**
 * 聊天消息 Repository。<p>
 * 封装消息实体的持久化操作，包括用户侧历史消息分页、管理后台消息查询及基于发送者和客户端 ID 的幂等查找。
 */
public interface ChatMessageRepository extends IService<ChatMessage> {

    /**
     * 统计用户在指定会话中可见的消息分页总数。
     */
    Long countMessagePage(Long conversationId, Long userId, Long beforeMessageId);

    /**
     * 查询用户在指定会话中可见的消息分页列表。
     */
    List<ChatMessageHistoryItem> selectMessagePage(Long conversationId, Long userId, Long beforeMessageId, Long offset, Long size);

    /**
     * 查询单条用户可见的消息详情。
     */
    ChatMessageHistoryItem selectVisibleMessageById(Long conversationId, Long userId, Long messageId);

    /**
     * 根据消息 ID 列表批量查询用户可见的消息详情。
     */
    List<ChatMessageHistoryItem> selectVisibleMessagesByIds(Long conversationId, Long userId, Collection<Long> messageIds);

    /**
     * 统计管理后台在指定会话中的消息分页总数。
     */
    Long countAdminMessagePage(Long conversationId, ChatAdminMessagePageQuery query);

    /**
     * 查询管理后台在指定会话中的消息分页列表。
     */
    List<ChatAdminMessageItem> selectAdminMessagePage(Long conversationId, ChatAdminMessagePageQuery query, Long offset, Long size);

    /**
     * 根据消息 ID 列表批量查询管理后台消息详情。
     */
    List<ChatAdminMessageItem> selectAdminMessagesByIds(Long conversationId, Collection<Long> messageIds);

    /**
     * 根据发送者 ID 和客户端消息 ID 查找消息，用于消息幂等去重。
     */
    ChatMessage findBySenderAndClientMessageId(Long senderId, String clientMessageId);

    /**
     * 查询指定用户在指定会话中的最新一条消息，用于慢速模式校验。
     */
    ChatMessage findLatestBySenderAndConversation(Long senderId, Long conversationId);
}
