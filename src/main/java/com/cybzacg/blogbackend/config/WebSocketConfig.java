package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.config.property.WebSocketProperties;
import com.cybzacg.blogbackend.config.websocket.AuthenticatedPrincipalHandshakeHandler;
import com.cybzacg.blogbackend.config.websocket.WebSocketAuthHandshakeInterceptor;
import com.cybzacg.blogbackend.module.chat.websocket.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册配置。<p>将聊天 WebSocket Handler 注册到指定端点，并绑定鉴权拦截器和自定义握手处理器。</p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketProperties webSocketProperties;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor;
    private final AuthenticatedPrincipalHandshakeHandler authenticatedPrincipalHandshakeHandler;

    /**
     * 注册 WebSocket 处理器及其拦截器、握手处理器和跨域配置。
     *
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, webSocketProperties.getEndpoint())
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setHandshakeHandler(authenticatedPrincipalHandshakeHandler)
                .setAllowedOriginPatterns(webSocketProperties.getAllowedOriginPatterns().toArray(String[]::new));
    }
}
