package com.cybzacg.blogbackend.module.chat.service;

import java.util.Collection;
import org.springframework.web.socket.WebSocketSession;

/**
 * 单机版 WebSocket 会话注册表。
 *
 * <p>用于维护当前节点在线用户的会话映射，后续可直接扩展单点消息推送与广播能力。
 */
public interface ChatWebSocketSessionRegistry {
    void register(WebSocketSession session);

    void unregister(WebSocketSession session);

    Collection<WebSocketSession> getSessions(Long userId);

    int getOnlineUserCount();

    int getSessionCount();
}
