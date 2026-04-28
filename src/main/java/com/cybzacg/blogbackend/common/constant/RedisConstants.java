package com.cybzacg.blogbackend.common.constant;

/**
 * Redis 相关常量。<p>集中管理 Key 分隔符、限流前缀和 WebSocket 推送 Topic。
 */
public final class RedisConstants {
    public static final String KEY_SEPARATOR = ":";
    public static final String IP_RATE_LIMIT_KEY_PREFIX = "security:ip:rate-limit";
    public static final String CHAT_WS_PUSH_TOPIC = "chat:ws:push";
    public static final String CHAT_SEND_RATE_LIMIT_KEY_PREFIX = "chat:send:rate-limit";

    public static final String XP_DAILY_KEY_PREFIX = "xp:daily";
    public static final String XP_IDEMPOTENT_KEY_PREFIX = "xp:idempotent";

    private RedisConstants() {
    }
}
