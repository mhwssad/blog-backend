package com.cybzacg.blogbackend.enums.chat;

import lombok.Getter;

/**
 * 聊天会话可见范围枚举。
 */
@Getter
public enum ChatConversationVisibilityScopeEnum {
    PUBLIC("public", "公开可见"),
    MEMBER("member", "成员可见"),
    PRIVATE("private", "私密可见");

    private final String code;
    private final String label;

    ChatConversationVisibilityScopeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ChatConversationVisibilityScopeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChatConversationVisibilityScopeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(String code) {
        return fromCode(code) != null;
    }
}
