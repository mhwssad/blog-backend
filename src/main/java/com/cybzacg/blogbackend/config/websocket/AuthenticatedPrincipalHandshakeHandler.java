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
 * WebSocket 握手用户绑定处理器。
 */
@Component
public class AuthenticatedPrincipalHandshakeHandler extends DefaultHandshakeHandler {
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
