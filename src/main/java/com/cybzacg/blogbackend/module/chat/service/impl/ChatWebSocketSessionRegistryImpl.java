package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import com.cybzacg.blogbackend.module.chat.service.ChatWebSocketSessionRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 本地会话注册表实现。
 */
@Service
public class ChatWebSocketSessionRegistryImpl implements ChatWebSocketSessionRegistry {
    private final Map<Long, Map<String, WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserIndex = new ConcurrentHashMap<>();

    @Override
    public void register(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId == null) {
            return;
        }
        sessionsByUser.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        sessionUserIndex.put(session.getId(), userId);
    }

    @Override
    public void unregister(WebSocketSession session) {
        Long userId = sessionUserIndex.remove(session.getId());
        if (userId == null) {
            userId = resolveUserId(session);
        }
        if (userId == null) {
            return;
        }
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session.getId());
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    @Override
    public Collection<WebSocketSession> getSessions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        return sessions == null ? List.of() : Collections.unmodifiableCollection(sessions.values());
    }

    @Override
    public int getOnlineUserCount() {
        return sessionsByUser.size();
    }

    @Override
    public int getSessionCount() {
        return sessionUserIndex.size();
    }

    private Long resolveUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(WebSocketConstants.ATTR_USER_ID);
        if (value instanceof Long userId) {
            return userId;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}


