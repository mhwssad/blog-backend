package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.module.chat.model.user.ChatConversationVO;

/**
 * 频道/私聊加入子服务：单聊打开、公开频道加入与退出。
 */
public interface ChatChannelJoinService {

    ChatConversationVO openSingleConversation(Long userId, Long targetUserId);

    ChatConversationVO joinConversation(Long userId, Long conversationId);

    void leaveConversation(Long userId, Long conversationId);
}
