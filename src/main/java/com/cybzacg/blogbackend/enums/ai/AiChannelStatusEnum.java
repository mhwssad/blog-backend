package com.cybzacg.blogbackend.enums.ai;

import lombok.Getter;

/**
 * AI 渠道状态枚举。
 */
@Getter
public enum AiChannelStatusEnum {
    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final Integer value;
    private final String label;

    AiChannelStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static AiChannelStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AiChannelStatusEnum item : values()) {
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
