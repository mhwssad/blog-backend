package com.cybzacg.blogbackend.config.websocket;

import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * WebSocket 握手用户绑定处理器。<p>在握手阶段从拦截器回填的属性中提取 Authentication，将其作为 WebSocket 会话的 Principal，便于后续获取当前用户信息。</p>
 */
@Component
public class AuthenticatedPrincipalHandshakeHandler extends DefaultHandshakeHandler {
    /**
     * 从握手属性中提取已认证的 Authentication 作为 WebSocket 会话的 Principal。
     *
     * @param request    服务端 HTTP 请求
     * @param wsHandler  WebSocket 处理器
     * @param attributes 握手属性
     * @return 已认证用户 Principal，未找到时回退到默认行为
     */
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object authentication = attributes.get(WebSocketConstants.ATTR_AUTHENTICATION);
        if (authentication instanceof Authentication auth) {
            return auth;
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}
