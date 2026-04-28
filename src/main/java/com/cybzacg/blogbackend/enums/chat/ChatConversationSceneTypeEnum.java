package com.cybzacg.blogbackend.enums.chat;

import lombok.Getter;

/**
 * 聊天会话场景枚举。
 */
@Getter
public enum ChatConversationSceneTypeEnum {
    SINGLE_CHAT("single_chat", "单聊"),
    USER_GROUP("user_group", "普通群聊"),
    HALL_CHANNEL("hall_channel", "大厅频道"),
    TOPIC_CHANNEL("topic_channel", "主题频道"),
    GLOBAL_CHANNEL("global_channel", "全站特殊频道");

    private final String code;
    private final String label;

    ChatConversationSceneTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ChatConversationSceneTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChatConversationSceneTypeEnum item : values()) {
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
