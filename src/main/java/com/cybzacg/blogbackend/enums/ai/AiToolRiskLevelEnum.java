package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 工具风险等级。
 */
@Getter
@AllArgsConstructor
public enum AiToolRiskLevelEnum {
    LOW("low", "低风险"),
    MEDIUM("medium", "中风险"),
    HIGH("high", "高风险");

    private final String code;
    private final String label;

    public static AiToolRiskLevelEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AiToolRiskLevelEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(String code) {
        return fromCode(code) != null;
    }
}
