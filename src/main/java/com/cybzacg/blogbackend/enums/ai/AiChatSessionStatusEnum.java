package com.cybzacg.blogbackend.enums.ai;

import lombok.Getter;

/**
 * AI 会话状态枚举。
 */
@Getter
public enum AiChatSessionStatusEnum {
    CLOSED(0, "关闭"),
    NORMAL(1, "正常");

    private final Integer value;
    private final String label;

    AiChatSessionStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static AiChatSessionStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AiChatSessionStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(Integer value) {
        return fromValue(value) != null;
    }
}
