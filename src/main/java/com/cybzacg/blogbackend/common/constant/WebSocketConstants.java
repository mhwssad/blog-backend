package com.cybzacg.blogbackend.common.constant;

/**
 * WebSocket 相关常量。<p>定义 WebSocket 握手阶段存入 Attributes 的键名。
 */
public final class WebSocketConstants {
    public static final String ATTR_AUTHENTICATION = "websocket.authentication";
    public static final String ATTR_USER_ID = "websocket.userId";
    public static final String ATTR_USERNAME = "websocket.username";

    private WebSocketConstants() {
    }
}
