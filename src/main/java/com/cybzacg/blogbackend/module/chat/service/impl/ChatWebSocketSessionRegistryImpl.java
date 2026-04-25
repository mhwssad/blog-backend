package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import com.cybzacg.blogbackend.module.chat.service.ChatWebSocketSessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 本地会话注册表实现。
 */
@Service
public class ChatWebSocketSessionRegistryImpl implements ChatWebSocketSessionRegistry {
    private final Map<Long, Map<String, WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserIndex = new ConcurrentHashMap<>();

    /**
     * 将 WebSocket 会话注册到本地会话表，按用户 ID 索引。
     */
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

    /**
     * 将 WebSocket 会话从本地会话表中移除，同时清理空的用户会话桶。
     */
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

    /**
     * 获取指定用户在当前节点上的全部活跃 WebSocket 会话。
     */
    @Override
    public Collection<WebSocketSession> getSessions(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Map<String, WebSocketSession> sessions = sessionsByUser.get(userId);
        return sessions == null ? List.of() : Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * 获取当前节点在线用户数。
     */
    @Override
    public int getOnlineUserCount() {
        return sessionsByUser.size();
    }

    /**
     * 获取当前节点 WebSocket 会话总数（含多端登录）。
     */
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


