package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMembersUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMessageType;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.websocket.ChatWebSocketMessageCodec;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 单机版聊天推送服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPushServiceImpl implements ChatPushService {
    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ChatWebSocketMessageCodec messageCodec;

    @Override
    public void pushMessageCreated(ChatMessageVO message, Collection<Long> userIds) {
        push(ChatWsMessageType.MESSAGE_CREATED.getValue(), message, userIds);
    }

    @Override
    public void pushMessageUpdated(ChatMessageVO message, Collection<Long> userIds) {
        push(ChatWsMessageType.MESSAGE_UPDATED.getValue(), message, userIds);
    }

    @Override
    public void pushMessageRevoked(ChatMessageVO message, Collection<Long> userIds) {
        push(ChatWsMessageType.MESSAGE_REVOKED.getValue(), message, userIds);
    }

    @Override
    public void pushReadUpdated(ChatReadStateVO readState, Collection<Long> userIds) {
        push(ChatWsMessageType.READ_UPDATED.getValue(), readState, userIds);
    }

    @Override
    public void pushConversationUpdated(ChatWsConversationUpdatedPayload payload, Collection<Long> userIds) {
        push(ChatWsMessageType.CONVERSATION_UPDATED.getValue(), payload, userIds);
    }

    @Override
    public void pushMembersUpdated(ChatWsMembersUpdatedPayload payload, Collection<Long> userIds) {
        push(ChatWsMessageType.MEMBERS_UPDATED.getValue(), payload, userIds);
    }

    private void push(String type, Object payload, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        TextMessage message = messageCodec.buildEvent(type, payload);
        Set<Long> distinctUserIds = new LinkedHashSet<>(userIds);
        for (Long userId : distinctUserIds) {
            for (WebSocketSession session : sessionRegistry.getSessions(userId)) {
                if (!session.isOpen()) {
                    continue;
                }
                try {
                    session.sendMessage(message);
                } catch (Exception ex) {
                    log.warn("push websocket message failed: type={}, userId={}, sessionId={}", type, userId, session.getId(), ex);
                }
            }
        }
    }
}
