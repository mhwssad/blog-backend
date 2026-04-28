package com.cybzacg.blogbackend.enums.ai;

import lombok.Getter;

/**
 * AI 调用成功状态枚举。
 */
@Getter
public enum AiUsageSuccessStatusEnum {
    FAILED(0, "失败"),
    SUCCESS(1, "成功");

    private final Integer value;
    private final String label;

    AiUsageSuccessStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static AiUsageSuccessStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AiUsageSuccessStatusEnum item : values()) {
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
