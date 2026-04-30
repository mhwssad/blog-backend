package com.cybzacg.blogbackend.module.chat.message.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatEditMessageRequest;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMarkReadRequest;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessagePageQuery;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;

/**
 * 消息生命周期子服务：编辑、撤回、删除、历史查询、已读标记。
 */
public interface ChatMessageLifecycleService {

    ChatMessageVO editMessage(Long userId, Long messageId, ChatEditMessageRequest request);

    void revokeMessage(Long userId, Long messageId);

    void deleteMessage(Long userId, Long messageId);

    PageResult<ChatMessageVO> pageMyMessages(Long userId, Long conversationId, ChatMessagePageQuery query);

    ChatReadStateVO markRead(Long userId, Long conversationId, ChatMarkReadRequest request);

    ChatReadStateVO markRead(Long userId, Long conversationId, Long readMessageId);
}
