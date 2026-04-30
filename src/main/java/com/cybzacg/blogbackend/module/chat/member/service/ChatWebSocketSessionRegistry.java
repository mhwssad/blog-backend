package com.cybzacg.blogbackend.module.chat.member.service;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;

/**
 * WebSocket 本地会话注册表。
 *
 * <p>只维护当前节点在线用户的会话映射；多节点广播由上层推送总线负责。
 */
public interface ChatWebSocketSessionRegistry {
    void register(WebSocketSession session);

    void unregister(WebSocketSession session);

    Collection<WebSocketSession> getSessions(Long userId);

    int getOnlineUserCount();

    int getSessionCount();
}
