package com.cybzacg.blogbackend.enums.experience;

import lombok.Getter;

/**
 * 经验来源类型枚举。
 */
@Getter
public enum ExperienceSourceTypeEnum {

    DAILY_LOGIN("daily_login", "每日登录",
            "xp.source.daily_login.value", "xp.source.daily_login.enabled", null),
    ARTICLE_PUBLISH("article_publish", "文章发布",
            "xp.source.article_publish.value", "xp.source.article_publish.enabled", null),
    COMMENT_CREATE("comment_create", "评论发布",
            "xp.source.comment_create.value", "xp.source.comment_create.enabled", "xp.daily.comment_create.cap"),
    LIKE_GIVEN("like_given", "主动点赞",
            "xp.source.like_given.value", "xp.source.like_given.enabled", null),
    LIKE_RECEIVED("like_received", "被点赞",
            "xp.source.like_received.value", "xp.source.like_received.enabled", null),
    CHAT_MESSAGE("chat_message", "聊天消息",
            "xp.source.chat_message.value", "xp.source.chat_message.enabled", "xp.daily.chat_message.cap");

    private final String value;
    private final String label;
    private final String configValueKey;
    private final String configEnabledKey;
    private final String dailyCapConfigKey;

    ExperienceSourceTypeEnum(String value, String label,
                             String configValueKey, String configEnabledKey,
                             String dailyCapConfigKey) {
        this.value = value;
        this.label = label;
        this.configValueKey = configValueKey;
        this.configEnabledKey = configEnabledKey;
        this.dailyCapConfigKey = dailyCapConfigKey;
    }

    public static ExperienceSourceTypeEnum fromValue(String value) {
        for (ExperienceSourceTypeEnum e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }

    public static boolean contains(String value) {
        return fromValue(value) != null;
    }
}
