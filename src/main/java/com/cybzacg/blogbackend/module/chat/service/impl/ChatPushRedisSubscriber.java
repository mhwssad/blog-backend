package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.module.chat.model.internal.ChatPushEventEnvelope;
import com.cybzacg.blogbackend.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 聊天 Redis 推送事件订阅器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPushRedisSubscriber implements MessageListener {
    private final ChatPushServiceImpl chatPushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            return;
        }
        try {
            ChatPushEventEnvelope event = JsonUtils.fromJson(new String(message.getBody(), StandardCharsets.UTF_8), ChatPushEventEnvelope.class);
            chatPushService.handleClusterEvent(event);
        } catch (Exception ex) {
            log.warn("handle chat redis push event failed", ex);
        }
    }
}
