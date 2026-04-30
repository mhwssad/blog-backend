package com.cybzacg.blogbackend.module.chat.push.service.impl;

import com.cybzacg.blogbackend.module.chat.shared.model.internal.ChatPushEventEnvelope;
import com.cybzacg.blogbackend.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatPushRedisSubscriberTest {
    @Mock
    private ChatPushServiceImpl chatPushService;

    @Test
    void onMessageShouldForwardValidRedisPayload() {
        ChatPushRedisSubscriber subscriber = new ChatPushRedisSubscriber(chatPushService);
        ChatPushEventEnvelope event = new ChatPushEventEnvelope();
        event.setOriginNodeId("node-a");
        event.setType("message_created");

        subscriber.onMessage(new DefaultMessage("chat:ws:push".getBytes(StandardCharsets.UTF_8),
                JsonUtils.toJson(event).getBytes(StandardCharsets.UTF_8)), null);

        verify(chatPushService).handleClusterEvent(argThat(payload ->
                payload != null
                        && "node-a".equals(payload.getOriginNodeId())
                        && "message_created".equals(payload.getType())
        ));
    }

    @Test
    void onMessageShouldIgnoreInvalidRedisPayload() {
        ChatPushRedisSubscriber subscriber = new ChatPushRedisSubscriber(chatPushService);

        subscriber.onMessage(new DefaultMessage("chat:ws:push".getBytes(StandardCharsets.UTF_8),
                "not-json".getBytes(StandardCharsets.UTF_8)), null);

        verify(chatPushService, never()).handleClusterEvent(argThat(event -> true));
    }
}
