package com.cybzacg.blogbackend.module.chat.push.service.impl;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.module.chat.shared.model.internal.ChatPushEventEnvelope;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsConversationUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMembersUpdatedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMessageDeletedPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMessageType;
import com.cybzacg.blogbackend.module.chat.push.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.member.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.websocket.codec.ChatWebSocketMessageCodec;
import com.cybzacg.blogbackend.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;

/**
 * 聊天推送服务实现。
 *
 * <p>当前采用“本地会话注册表 + Redis pub/sub”的模式：
 * 当前节点先把事件推给本地会话，再把事件广播到 Redis，由其他节点转发到各自本地会话。
 */
@Slf4j
@Service
public class ChatPushServiceImpl implements ChatPushService {
    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ChatWebSocketMessageCodec messageCodec;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String localNodeId;

    /**
     * 仅供 Spring 测试上下文使用，实际依赖通过有参构造器注入。
     */
    ChatPushServiceImpl() {
        this(null, null, null, UUID.randomUUID().toString());
    }

    public ChatPushServiceImpl(ChatWebSocketSessionRegistry sessionRegistry,
                               ChatWebSocketMessageCodec messageCodec,
                               RedisTemplate<String, Object> redisTemplate) {
        this(sessionRegistry, messageCodec, redisTemplate, UUID.randomUUID().toString());
    }

    ChatPushServiceImpl(ChatWebSocketSessionRegistry sessionRegistry,
                        ChatWebSocketMessageCodec messageCodec,
                        RedisTemplate<String, Object> redisTemplate,
                        String localNodeId) {
        this.sessionRegistry = sessionRegistry;
        this.messageCodec = messageCodec;
        this.redisTemplate = redisTemplate;
        this.localNodeId = localNodeId;
    }

    /**
     * 向指定用户推送新消息通知。
     */
    @Override
    public void pushMessageCreated(ChatMessageVO message, Collection<Long> userIds) {
        pushAndBroadcast(ChatWsMessageType.MESSAGE_CREATED.getValue(), message, userIds);
    }

    /**
     * 向指定用户推送消息更新通知（如附件处理完成）。
     */
    @Override
    public void pushMessageUpdated(ChatMessageVO message, Collection<Long> userIds) {
        pushAndBroadcast(ChatWsMessageType.MESSAGE_UPDATED.getValue(), message, userIds);
    }

    /**
     * 向指定用户推送消息撤回通知。
     */
    @Override
    public void pushMessageRevoked(ChatMessageVO message, Collection<Long> userIds) {
        pushAndBroadcast(ChatWsMessageType.MESSAGE_REVOKED.getValue(), message, userIds);
    }

    /**
     * 向指定用户推送消息删除通知。
     */
    @Override
    public void pushMessageDeleted(ChatWsMessageDeletedPayload payload, Collection<Long> userIds) {
        pushAndBroadcast(ChatWsMessageType.MESSAGE_DELETED.getValue(), payload, userIds);
    }

    /**
     * 向指定用户推送已读状态更新通知。
     */
    @Override
    public void pushReadUpdated(ChatReadStateVO readState, Collection<Long> userIds) {
        pushAndBroadcast(ChatWsMessageType.READ_UPDATED.getValue(), readState, userIds);
    }

    /**
     * 向指定用户推送会话信息变更通知（如群名、公告、状态变更）。
     */
    @Override
    public void pushConversationUpdated(ChatWsConversationUpdatedPayload payload, Collection<Long> userIds) {
        pushAndBroadcast(ChatWsMessageType.CONVERSATION_UPDATED.getValue(), payload, userIds);
    }

    /**
     * 向指定用户推送群成员变更通知（如加入、移除、角色变更）。
     */
    @Override
    public void pushMembersUpdated(ChatWsMembersUpdatedPayload payload, Collection<Long> userIds) {
        pushAndBroadcast(ChatWsMessageType.MEMBERS_UPDATED.getValue(), payload, userIds);
    }

    /**
     * 处理 Redis 订阅到的跨节点推送事件，仅把事件下发到当前节点的本地会话。
     */
    void handleClusterEvent(ChatPushEventEnvelope event) {
        if (event == null
                || !StringUtils.hasText(event.getType())
                || event.getUserIds() == null
                || event.getUserIds().isEmpty()
                || localNodeId.equals(event.getOriginNodeId())) {
            return;
        }
        Object payload = decodeClusterPayload(event);
        if (payload == null) {
            return;
        }
        pushLocal(event.getType(), payload, event.getUserIds());
    }

    private void pushAndBroadcast(String type, Object payload, Collection<Long> userIds) {
        List<Long> distinctUserIds = distinctUserIds(userIds);
        if (distinctUserIds.isEmpty()) {
            return;
        }
        pushLocal(type, payload, distinctUserIds);
        publishClusterEvent(type, payload, distinctUserIds);
    }

    private void publishClusterEvent(String type, Object payload, List<Long> userIds) {
        ChatPushEventEnvelope event = new ChatPushEventEnvelope();
        event.setOriginNodeId(localNodeId);
        event.setType(type);
        event.setUserIds(userIds);
        event.setPayload(JsonUtils.getObjectMapper().valueToTree(payload));
        try {
            redisTemplate.convertAndSend(RedisConstants.CHAT_WS_PUSH_TOPIC, event);
        } catch (Exception ex) {
            log.warn("publish chat redis push event failed: type={}, userIds={}", type, userIds, ex);
        }
    }

    private void pushLocal(String type, Object payload, Collection<Long> userIds) {
        TextMessage message = messageCodec.buildEvent(type, payload);
        for (Long userId : distinctUserIds(userIds)) {
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

    private Object decodeClusterPayload(ChatPushEventEnvelope event) {
        if (event.getPayload() == null || event.getPayload().isNull()) {
            return null;
        }
        return switch (event.getType()) {
            case "message_created", "message_updated", "message_revoked" ->
                    JsonUtils.getObjectMapper().convertValue(event.getPayload(), ChatMessageVO.class);
            case "message_deleted" ->
                    JsonUtils.getObjectMapper().convertValue(event.getPayload(), ChatWsMessageDeletedPayload.class);
            case "read_updated" -> JsonUtils.getObjectMapper().convertValue(event.getPayload(), ChatReadStateVO.class);
            case "conversation_updated" ->
                    JsonUtils.getObjectMapper().convertValue(event.getPayload(), ChatWsConversationUpdatedPayload.class);
            case "members_updated" ->
                    JsonUtils.getObjectMapper().convertValue(event.getPayload(), ChatWsMembersUpdatedPayload.class);
            default -> {
                log.warn("ignore unknown chat redis push event type: {}", event.getType());
                yield null;
            }
        };
    }

    private List<Long> distinctUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<Long> distinctUserIds = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                distinctUserIds.add(userId);
            }
        }
        return distinctUserIds.isEmpty() ? List.of() : List.copyOf(distinctUserIds);
    }
}
