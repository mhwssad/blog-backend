package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import com.cybzacg.blogbackend.module.chat.member.service.impl.ChatWebSocketSessionRegistryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatWebSocketSessionRegistryImplTest {
    private final ChatWebSocketSessionRegistryImpl registry = new ChatWebSocketSessionRegistryImpl();

    @Test
    void registerAndUnregisterShouldMaintainSessionAndOnlineCounts() {
        WebSocketSession firstSession = mock(WebSocketSession.class);
        WebSocketSession secondSession = mock(WebSocketSession.class);

        when(firstSession.getId()).thenReturn("s-1");
        when(firstSession.getAttributes()).thenReturn(Map.of(WebSocketConstants.ATTR_USER_ID, 1L));
        when(secondSession.getId()).thenReturn("s-2");
        when(secondSession.getAttributes()).thenReturn(Map.of(WebSocketConstants.ATTR_USER_ID, 1L));

        registry.register(firstSession);
        registry.register(secondSession);

        assertEquals(1, registry.getOnlineUserCount());
        assertEquals(2, registry.getSessionCount());
        assertEquals(2, registry.getSessions(1L).size());

        registry.unregister(firstSession);

        assertEquals(1, registry.getOnlineUserCount());
        assertEquals(1, registry.getSessionCount());
        assertEquals(1, registry.getSessions(1L).size());

        registry.unregister(secondSession);

        assertEquals(0, registry.getOnlineUserCount());
        assertEquals(0, registry.getSessionCount());
        assertTrue(registry.getSessions(1L).isEmpty());
    }

    @Test
    void registerShouldAcceptNumericUserIdAttribute() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s-3");
        when(session.getAttributes()).thenReturn(Map.of(WebSocketConstants.ATTR_USER_ID, Integer.valueOf(2)));

        registry.register(session);

        assertEquals(1, registry.getOnlineUserCount());
        assertEquals(1, registry.getSessionCount());
        assertEquals(1, registry.getSessions(2L).size());
    }

    @Test
    void registerShouldIgnoreSessionWithoutResolvableUserId() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s-4");
        when(session.getAttributes()).thenReturn(Map.of(WebSocketConstants.ATTR_USER_ID, "2"));

        registry.register(session);

        assertEquals(0, registry.getOnlineUserCount());
        assertEquals(0, registry.getSessionCount());
        assertTrue(registry.getSessions(2L).isEmpty());
    }
}
