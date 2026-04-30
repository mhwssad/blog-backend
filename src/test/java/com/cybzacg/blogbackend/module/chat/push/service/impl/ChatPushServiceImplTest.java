package com.cybzacg.blogbackend.module.chat.push.service.impl;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.member.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.shared.model.internal.ChatPushEventEnvelope;
import com.cybzacg.blogbackend.module.chat.websocket.codec.ChatWebSocketMessageCodec;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMembersUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMessageDeletedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMessageType;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatPushServiceImplTest {
    @Mock
    private ChatWebSocketSessionRegistry sessionRegistry;
    @Mock
    private ChatWebSocketMessageCodec messageCodec;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private WebSocketSession session;

    private ChatPushServiceImpl chatPushService;

    @BeforeEach
    void setUp() {
        chatPushService = new ChatPushServiceImpl(sessionRegistry, messageCodec, redisTemplate, "node-a");
    }

    @Test
    void pushMessageCreatedShouldSendLocalAndPublishRedisEvent() throws Exception {
        ChatMessageVO payload = new ChatMessageVO();
        payload.setId(9001L);
        TextMessage wsMessage = new TextMessage("{\"type\":\"message_created\"}");

        when(messageCodec.buildEvent(ChatWsMessageType.MESSAGE_CREATED.getValue(), payload)).thenReturn(wsMessage);
        when(sessionRegistry.getSessions(1L)).thenReturn(List.of(session));
        when(sessionRegistry.getSessions(2L)).thenReturn(List.of());
        when(session.isOpen()).thenReturn(true);

        chatPushService.pushMessageCreated(payload, List.of(1L, 1L, 2L));

        verify(session).sendMessage(wsMessage);
        verify(redisTemplate).convertAndSend(eq(RedisConstants.CHAT_WS_PUSH_TOPIC), argThat((ChatPushEventEnvelope event) ->
                event != null
                        && "node-a".equals(event.getOriginNodeId())
                        && ChatWsMessageType.MESSAGE_CREATED.getValue().equals(event.getType())
                        && List.of(1L, 2L).equals(event.getUserIds())
                        && event.getPayload() != null
                        && Long.valueOf(9001L).equals(JsonUtils.getObjectMapper()
                        .convertValue(event.getPayload(), ChatMessageVO.class).getId())
        ));
    }

    @Test
    void pushMessageUpdatedShouldSendLocalAndPublishRedisEvent() throws Exception {
        ChatMessageVO payload = new ChatMessageVO();
        payload.setId(9003L);
        assertPushAndBroadcast(ChatWsMessageType.MESSAGE_UPDATED, payload, ChatMessageVO.class, 9003L);
    }

    @Test
    void pushMessageRevokedShouldSendLocalAndPublishRedisEvent() throws Exception {
        ChatMessageVO payload = new ChatMessageVO();
        payload.setId(9004L);
        assertPushAndBroadcast(ChatWsMessageType.MESSAGE_REVOKED, payload, ChatMessageVO.class, 9004L);
    }

    @Test
    void pushMessageDeletedShouldSendLocalAndPublishRedisEvent() throws Exception {
        ChatWsMessageDeletedPayload payload = ChatWsMessageDeletedPayload.builder()
                .conversationId(1001L)
                .messageId(9005L)
                .userId(1L)
                .unreadCount(3)
                .build();
        assertPushAndBroadcast(ChatWsMessageType.MESSAGE_DELETED, payload, ChatWsMessageDeletedPayload.class, 9005L);
    }

    @Test
    void pushReadUpdatedShouldSendLocalAndPublishRedisEvent() throws Exception {
        ChatReadStateVO payload = new ChatReadStateVO();
        payload.setConversationId(1001L);
        payload.setReadMessageId(9006L);
        assertPushAndBroadcast(ChatWsMessageType.READ_UPDATED, payload, ChatReadStateVO.class, 9006L);
    }

    @Test
    void pushConversationUpdatedShouldSendLocalAndPublishRedisEvent() throws Exception {
        ChatWsConversationUpdatedPayload payload = ChatWsConversationUpdatedPayload.builder()
                .action("notice_updated")
                .conversationId(1001L)
                .ownerId(2L)
                .build();
        assertPushAndBroadcast(ChatWsMessageType.CONVERSATION_UPDATED, payload, ChatWsConversationUpdatedPayload.class, 1001L);
    }

    @Test
    void pushMembersUpdatedShouldSendLocalAndPublishRedisEvent() throws Exception {
        ChatWsMembersUpdatedPayload payload = ChatWsMembersUpdatedPayload.builder()
                .action("members_invited")
                .conversationId(1001L)
                .affectedUserId(2L)
                .members(List.of(new ChatMemberVO()))
                .build();
        assertPushAndBroadcast(ChatWsMessageType.MEMBERS_UPDATED, payload, ChatWsMembersUpdatedPayload.class, 1001L);
    }

    @Test
    void handleClusterEventShouldIgnoreCurrentNodeEvent() {
        ChatPushEventEnvelope event = new ChatPushEventEnvelope();
        event.setOriginNodeId("node-a");
        event.setType(ChatWsMessageType.MESSAGE_CREATED.getValue());
        event.setUserIds(List.of(1L));
        event.setPayload(JsonUtils.getObjectMapper().valueToTree(new ChatMessageVO()));

        chatPushService.handleClusterEvent(event);

        verifyNoInteractions(messageCodec);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void handleClusterEventShouldDispatchRemoteEventToLocalSessions() throws Exception {
        ChatMessageVO payload = new ChatMessageVO();
        payload.setId(9002L);
        TextMessage wsMessage = new TextMessage("{\"type\":\"message_created\"}");

        when(messageCodec.buildEvent(eq(ChatWsMessageType.MESSAGE_CREATED.getValue()), any())).thenReturn(wsMessage);
        when(sessionRegistry.getSessions(2L)).thenReturn(List.of(session));
        when(session.isOpen()).thenReturn(true);

        ChatPushEventEnvelope event = new ChatPushEventEnvelope();
        event.setOriginNodeId("node-b");
        event.setType(ChatWsMessageType.MESSAGE_CREATED.getValue());
        event.setUserIds(List.of(2L));
        event.setPayload(JsonUtils.getObjectMapper().valueToTree(payload));

        chatPushService.handleClusterEvent(event);

        verify(session).sendMessage(wsMessage);
        verify(redisTemplate, never()).convertAndSend(any(), any());
        verify(messageCodec).buildEvent(eq(ChatWsMessageType.MESSAGE_CREATED.getValue()), argThat(body ->
                body instanceof ChatMessageVO messageVO && Long.valueOf(9002L).equals(messageVO.getId())
        ));
    }

    @Test
    void handleClusterEventShouldIgnoreUnknownEventType() {
        ChatPushEventEnvelope event = new ChatPushEventEnvelope();
        event.setOriginNodeId("node-b");
        event.setType("unknown_event");
        event.setUserIds(List.of(2L));
        event.setPayload(JsonUtils.getObjectMapper().createObjectNode().put("id", 1L));

        chatPushService.handleClusterEvent(event);

        verifyNoInteractions(messageCodec);
        verify(redisTemplate, never()).convertAndSend(any(), any());
    }

    private <T> void assertPushAndBroadcast(ChatWsMessageType type,
                                            T payload,
                                            Class<T> payloadClass,
                                            Long identity) throws Exception {
        TextMessage wsMessage = new TextMessage("{\"type\":\"" + type.getValue() + "\"}");

        when(messageCodec.buildEvent(type.getValue(), payload)).thenReturn(wsMessage);
        when(sessionRegistry.getSessions(1L)).thenReturn(List.of(session));
        when(sessionRegistry.getSessions(2L)).thenReturn(List.of());
        when(session.isOpen()).thenReturn(true);

        switch (type) {
            case MESSAGE_UPDATED -> chatPushService.pushMessageUpdated((ChatMessageVO) payload, List.of(1L, 2L, 1L));
            case MESSAGE_REVOKED -> chatPushService.pushMessageRevoked((ChatMessageVO) payload, List.of(1L, 2L, 1L));
            case MESSAGE_DELETED ->
                    chatPushService.pushMessageDeleted((ChatWsMessageDeletedPayload) payload, List.of(1L, 2L, 1L));
            case READ_UPDATED -> chatPushService.pushReadUpdated((ChatReadStateVO) payload, List.of(1L, 2L, 1L));
            case CONVERSATION_UPDATED ->
                    chatPushService.pushConversationUpdated((ChatWsConversationUpdatedPayload) payload, List.of(1L, 2L, 1L));
            case MEMBERS_UPDATED ->
                    chatPushService.pushMembersUpdated((ChatWsMembersUpdatedPayload) payload, List.of(1L, 2L, 1L));
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        }

        verify(session).sendMessage(wsMessage);
        verify(redisTemplate).convertAndSend(eq(RedisConstants.CHAT_WS_PUSH_TOPIC), argThat((ChatPushEventEnvelope event) ->
                event != null
                        && "node-a".equals(event.getOriginNodeId())
                        && type.getValue().equals(event.getType())
                        && List.of(1L, 2L).equals(event.getUserIds())
                        && event.getPayload() != null
                        && extractIdentity(payloadClass, event.getPayload()) != null
                        && identity.equals(extractIdentity(payloadClass, event.getPayload()))
        ));
    }

    private <T> Long extractIdentity(Class<T> payloadClass, Object payload) {
        JsonNode payloadNode = JsonUtils.getObjectMapper().valueToTree(payload);
        if (payloadClass == ChatWsMessageDeletedPayload.class) {
            return payloadNode.path("messageId").isNumber() ? payloadNode.path("messageId").longValue() : null;
        }
        if (payloadClass == ChatWsConversationUpdatedPayload.class || payloadClass == ChatWsMembersUpdatedPayload.class) {
            return payloadNode.path("conversationId").isNumber() ? payloadNode.path("conversationId").longValue() : null;
        }
        T converted = JsonUtils.getObjectMapper().convertValue(payloadNode, payloadClass);
        if (converted instanceof ChatMessageVO messageVO) {
            return messageVO.getId();
        }
        if (converted instanceof ChatReadStateVO readStateVO) {
            return readStateVO.getReadMessageId();
        }
        return null;
    }
}
