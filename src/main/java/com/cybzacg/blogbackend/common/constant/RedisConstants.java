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

    public static final String FOLLOW_COUNT_CACHE_PREFIX = "follow:count";

    public static final String AI_QUOTA_USER_DAILY_PREFIX = "ai:quota:user";
    public static final String AI_QUOTA_PLATFORM_DAILY_PREFIX = "ai:quota:platform";

    public static final String AI_KNOWLEDGE_SYNC_LOCK_PREFIX = "ai:knowledge:sync:lock";
    public static final String AI_KNOWLEDGE_SOURCE_CONFIG_CACHE_PREFIX = "ai:knowledge:source-config";
    public static final String AI_RAG_ACTIVE_CHUNKS_CACHE_KEY = "ai:rag:chunks:active";
    public static final String AI_RAG_SEARCH_CACHE_PREFIX = "ai:rag:search";
    public static final String AI_TOOL_EXECUTE_RATE_LIMIT_PREFIX = "ai:tool:execute:rate-limit";

    public static final String AI_ACCOUNT_POOL_USAGE_PREFIX = "ai:account-pool:usage";
    public static final String AI_ACCOUNT_POOL_RECOVER_LOCK = "ai:account-pool:recover:lock";

    public static final String DASHBOARD_CACHE_PREFIX = "dashboard";

    private RedisConstants() {
    }
}
