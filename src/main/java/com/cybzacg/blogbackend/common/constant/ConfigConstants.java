package com.cybzacg.blogbackend.common.constant;

/**
 * 系统配置项常量。<p>集中管理动态配置的缓存 Key、配置 Key 和默认值，便于统一维护。
 */
public final class ConfigConstants {
    public static final String CACHE_KEY_PREFIX = "sys:config";
    public static final String SECURITY_IP_RATE_LIMIT_PER_SECOND_NAME = "全局IP每秒请求限流阈值";
    public static final String SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY = "security.ip.rate-limit.per-second";
    public static final int DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND = 10;
    public static final String AUTH_LOGIN_FAIL_MAX_ATTEMPTS_NAME = "登录失败锁定阈值";
    public static final String AUTH_LOGIN_FAIL_MAX_ATTEMPTS_KEY = "auth.login-fail.max-attempts";
    public static final int DEFAULT_AUTH_LOGIN_FAIL_MAX_ATTEMPTS = 5;
    public static final String AUTH_LOGIN_FAIL_LOCK_MINUTES_NAME = "登录失败锁定时长(分钟)";
    public static final String AUTH_LOGIN_FAIL_LOCK_MINUTES_KEY = "auth.login-fail.lock-minutes";
    public static final int DEFAULT_AUTH_LOGIN_FAIL_LOCK_MINUTES = 15;
    public static final String CHAT_SEND_RATE_LIMIT_PER_MINUTE_NAME = "聊天用户每分钟发送限流阈值";
    public static final String CHAT_SEND_RATE_LIMIT_PER_MINUTE_KEY = "chat.send.rate-limit.per-minute";
    public static final int DEFAULT_CHAT_SEND_RATE_LIMIT_PER_MINUTE = 30;
    public static final String CHAT_SENSITIVE_WORDS_NAME = "聊天敏感词列表";
    public static final String CHAT_SENSITIVE_WORDS_KEY = "chat.sensitive-words";

    private ConfigConstants() {
    }
}
