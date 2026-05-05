package com.cybzacg.blogbackend.enums.report;

import lombok.Getter;

/**
 * 举报对象类型枚举。
 */
@Getter
public enum ReportTargetTypeEnum {
    ARTICLE("article", "文章"),
    COMMENT("comment", "评论"),
    CHAT_MESSAGE("chat_message", "聊天消息"),
    FORUM_POST("forum_post", "论坛帖子"),
    FORUM_REPLY("forum_reply", "论坛回复");

    private final String code;
    private final String label;

    ReportTargetTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ReportTargetTypeEnum fromCode(String code) {
        if (code == null) return null;
        for (ReportTargetTypeEnum item : values()) {
            if (item.code.equals(code)) return item;
        }
        return null;
    }

    public static boolean contains(String code) {
        return fromCode(code) != null;
    }
}
