package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMembersUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMessageDeletedPayload;
import java.util.Collection;

/**
 * 聊天实时推送服务。
 */
public interface ChatPushService {
    void pushMessageCreated(ChatMessageVO message, Collection<Long> userIds);

    void pushMessageUpdated(ChatMessageVO message, Collection<Long> userIds);

    void pushMessageRevoked(ChatMessageVO message, Collection<Long> userIds);

    void pushMessageDeleted(ChatWsMessageDeletedPayload payload, Collection<Long> userIds);

    void pushReadUpdated(ChatReadStateVO readState, Collection<Long> userIds);

    void pushConversationUpdated(ChatWsConversationUpdatedPayload payload, Collection<Long> userIds);

    void pushMembersUpdated(ChatWsMembersUpdatedPayload payload, Collection<Long> userIds);
}
