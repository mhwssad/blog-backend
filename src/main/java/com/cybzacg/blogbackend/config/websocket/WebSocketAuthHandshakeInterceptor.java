package com.cybzacg.blogbackend.config.websocket;

import com.cybzacg.blogbackend.common.constant.HttpHeaderConstants;
import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import com.cybzacg.blogbackend.config.property.WebSocketProperties;
import com.cybzacg.blogbackend.module.auth.token.TokenManager;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket 握手鉴权拦截器。<p>复用现有 TokenManager，在握手阶段从请求头或查询参数中提取访问令牌并完成校验，将用户认证信息回填到握手属性中。</p>
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {
    private final TokenManager tokenManager;
    private final WebSocketProperties webSocketProperties;

    /**
     * 握手前校验访问令牌，校验通过则将用户认证信息写入握手属性。
     *
     * @param request    服务端 HTTP 请求
     * @param response   服务端 HTTP 响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 握手属性，用于传递用户信息
     * @return 令牌校验通过返回 true，否则返回 false 并设置 401 状态码
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token) || !tokenManager.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Authentication authentication = tokenManager.parseToken(token);
        Long userId = SecurityUtils.getUserId(authentication);
        if (userId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(WebSocketConstants.ATTR_AUTHENTICATION, authentication);
        attributes.put(WebSocketConstants.ATTR_USER_ID, userId);
        attributes.put(WebSocketConstants.ATTR_USERNAME, SecurityUtils.getUsername(authentication));
        return true;
    }

    /**
     * 握手完成后的回调，当前无额外处理。
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private String resolveToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(HttpHeaderConstants.AUTHORIZATION);
        if (StringUtils.hasText(token)) {
            return token;
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter(webSocketProperties.getTokenQueryParam());
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        URI uri = request.getURI();
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        return queryParams.getFirst(webSocketProperties.getTokenQueryParam());
    }
}


