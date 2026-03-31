package com.cybzacg.blogbackend.common.constant;

/**
 * Redis 相关常量
 */
public final class RedisConstants {
    public static final String KEY_SEPARATOR = ":";
    public static final String IP_RATE_LIMIT_KEY_PREFIX = "security:ip:rate-limit";
    public static final String CHAT_WS_PUSH_TOPIC = "chat:ws:push";
    public static final String CHAT_SEND_RATE_LIMIT_KEY_PREFIX = "chat:send:rate-limit";

    private RedisConstants() {
    }
}
