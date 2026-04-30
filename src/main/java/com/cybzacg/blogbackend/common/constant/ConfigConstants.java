package com.cybzacg.blogbackend.common.constant;

/**
 * 系统配置项常量。<p>集中管理动态配置的缓存 Key、配置 Key 和默认值，便于统一维护。
 */
public final class ConfigConstants {
    public static final String CACHE_KEY_PREFIX = "sys:config";
    public static final String SECURITY_IP_RATE_LIMIT_PER_SECOND_NAME = "全局IP每秒请求限流阈值";
    public static final String SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY = "security.ip.rate-limit.per-second";
    public static final int DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND = 100;
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
    public static final String CHAT_HALL_SPEAK_MIN_LEVEL_NAME = "大厅发言最低等级";
    public static final String CHAT_HALL_SPEAK_MIN_LEVEL_KEY = "chat.hall.speak.min-level";
    public static final int DEFAULT_CHAT_HALL_SPEAK_MIN_LEVEL = 1;
    public static final String CHAT_GROUP_CREATE_MIN_LEVEL_NAME = "建群最低等级";
    public static final String CHAT_GROUP_CREATE_MIN_LEVEL_KEY = "chat.group.create.min-level";
    public static final int DEFAULT_CHAT_GROUP_CREATE_MIN_LEVEL = 2;
    public static final String CHAT_GROUP_CREATE_MAX_COUNT_NAME = "用户可创建群聊数量上限";
    public static final String CHAT_GROUP_CREATE_MAX_COUNT_KEY = "chat.group.create.max-count";
    public static final int DEFAULT_CHAT_GROUP_CREATE_MAX_COUNT = 20;
    public static final String CHAT_CHANNEL_CREATE_APPLICATION_MIN_LEVEL_NAME = "频道创建申请最低等级";
    public static final String CHAT_CHANNEL_CREATE_APPLICATION_MIN_LEVEL_KEY = "chat.channel-create-application.min-level";
    public static final int DEFAULT_CHAT_CHANNEL_CREATE_APPLICATION_MIN_LEVEL = 2;
    public static final String ARTICLE_MAX_COUNT_NORMAL_USER_NAME = "普通用户文章总量上限";
    public static final String ARTICLE_MAX_COUNT_NORMAL_USER_KEY = "article.max-count.normal-user";
    public static final int DEFAULT_ARTICLE_MAX_COUNT_NORMAL_USER = 20;
    public static final String ARTICLE_MAX_COUNT_AUTHOR_NAME = "作者文章总量上限";
    public static final String ARTICLE_MAX_COUNT_AUTHOR_KEY = "article.max-count.author";
    public static final int DEFAULT_ARTICLE_MAX_COUNT_AUTHOR = 200;

    // ========== 经验体系配置 ==========

    public static final String XP_SOURCE_DAILY_LOGIN_VALUE_KEY = "xp.source.daily_login.value";
    public static final int DEFAULT_XP_SOURCE_DAILY_LOGIN_VALUE = 10;
    public static final String XP_SOURCE_ARTICLE_PUBLISH_VALUE_KEY = "xp.source.article_publish.value";
    public static final int DEFAULT_XP_SOURCE_ARTICLE_PUBLISH_VALUE = 20;
    public static final String XP_SOURCE_COMMENT_CREATE_VALUE_KEY = "xp.source.comment_create.value";
    public static final int DEFAULT_XP_SOURCE_COMMENT_CREATE_VALUE = 5;
    public static final String XP_SOURCE_LIKE_GIVEN_VALUE_KEY = "xp.source.like_given.value";
    public static final int DEFAULT_XP_SOURCE_LIKE_GIVEN_VALUE = 2;
    public static final String XP_SOURCE_LIKE_RECEIVED_VALUE_KEY = "xp.source.like_received.value";
    public static final int DEFAULT_XP_SOURCE_LIKE_RECEIVED_VALUE = 3;
    public static final String XP_SOURCE_CHAT_MESSAGE_VALUE_KEY = "xp.source.chat_message.value";
    public static final int DEFAULT_XP_SOURCE_CHAT_MESSAGE_VALUE = 1;

    public static final String XP_SOURCE_DAILY_LOGIN_ENABLED_KEY = "xp.source.daily_login.enabled";
    public static final String XP_SOURCE_ARTICLE_PUBLISH_ENABLED_KEY = "xp.source.article_publish.enabled";
    public static final String XP_SOURCE_COMMENT_CREATE_ENABLED_KEY = "xp.source.comment_create.enabled";
    public static final String XP_SOURCE_LIKE_GIVEN_ENABLED_KEY = "xp.source.like_given.enabled";
    public static final String XP_SOURCE_LIKE_RECEIVED_ENABLED_KEY = "xp.source.like_received.enabled";
    public static final String XP_SOURCE_CHAT_MESSAGE_ENABLED_KEY = "xp.source.chat_message.enabled";

    public static final String XP_DAILY_TOTAL_CAP_KEY = "xp.daily.total-cap";
    public static final int DEFAULT_XP_DAILY_TOTAL_CAP = 200;
    public static final String XP_DAILY_CHAT_MESSAGE_CAP_KEY = "xp.daily.chat_message.cap";
    public static final int DEFAULT_XP_DAILY_CHAT_MESSAGE_CAP = 30;
    public static final String XP_DAILY_COMMENT_CREATE_CAP_KEY = "xp.daily.comment_create.cap";
    public static final int DEFAULT_XP_DAILY_COMMENT_CREATE_CAP = 50;

    // ========== AI 配置 ==========

    public static final String AI_GLOBAL_ENABLED_KEY = "ai.global.enabled";
    public static final String AI_GLOBAL_ENABLED_NAME = "AI全局开关";
    public static final String DEFAULT_AI_GLOBAL_ENABLED = "false";
    public static final String AI_PLATFORM_DAILY_QUOTA_KEY = "ai.platform.daily-quota";
    public static final int DEFAULT_AI_PLATFORM_DAILY_QUOTA = 1000;

    private ConfigConstants() {
    }
}
