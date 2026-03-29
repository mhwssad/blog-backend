package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.config.property.WebSocketProperties;
import com.cybzacg.blogbackend.config.websocket.AuthenticatedPrincipalHandshakeHandler;
import com.cybzacg.blogbackend.config.websocket.WebSocketAuthHandshakeInterceptor;
import com.cybzacg.blogbackend.module.chat.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 基础配置。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketProperties webSocketProperties;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor;
    private final AuthenticatedPrincipalHandshakeHandler authenticatedPrincipalHandshakeHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, webSocketProperties.getEndpoint())
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setHandshakeHandler(authenticatedPrincipalHandshakeHandler)
                .setAllowedOriginPatterns(webSocketProperties.getAllowedOriginPatterns().toArray(String[]::new));
    }
}
