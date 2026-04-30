package com.cybzacg.blogbackend.module.chat.conversation.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationPageQuery;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatLobbyMessageVO;

/**
 * 会话查询子服务：会话列表分页、会话详情、大厅消息。
 */
public interface ChatConversationQueryService {

    PageResult<ChatConversationVO> pageMyConversations(Long userId, ChatConversationPageQuery query);

    ChatConversationVO getMyConversation(Long userId, Long conversationId);

    PageResult<ChatLobbyMessageVO> pageLobbyMessages(Long current, Long size, Long beforeMessageId);
}
