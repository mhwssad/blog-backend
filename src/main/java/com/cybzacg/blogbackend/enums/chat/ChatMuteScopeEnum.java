package com.cybzacg.blogbackend.enums.chat;

/**
 * 禁言范围枚举。
 */
public enum ChatMuteScopeEnum {

    GLOBAL("global", "全站"),
    LOBBY("lobby", "大厅频道"),
    TOPIC_CHANNEL("topic_channel", "主题频道"),
    GROUP("group", "群聊");

    public final String code;
    private final String label;

    ChatMuteScopeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据 code 获取枚举值。
     */
    public static ChatMuteScopeEnum fromCode(String code) {
        for (ChatMuteScopeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 判断 code 是否为有效枚举值。
     */
    public static boolean contains(String code) {
        return fromCode(code) != null;
    }
}