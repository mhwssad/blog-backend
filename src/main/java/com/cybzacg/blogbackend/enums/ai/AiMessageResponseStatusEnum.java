package com.cybzacg.blogbackend.enums.ai;

import lombok.Getter;

/**
 * AI 消息响应状态枚举。
 */
@Getter
public enum AiMessageResponseStatusEnum {
    FAILED(0, "失败"),
    SUCCESS(1, "成功");

    private final Integer value;
    private final String label;

    AiMessageResponseStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static AiMessageResponseStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AiMessageResponseStatusEnum item : values()) {
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
