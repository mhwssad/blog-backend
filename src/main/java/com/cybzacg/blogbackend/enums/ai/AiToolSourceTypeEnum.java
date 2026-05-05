package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 工具来源类型。
 */
@Getter
@AllArgsConstructor
public enum AiToolSourceTypeEnum {
    BUILTIN("builtin", "内置工具"),
    MCP("mcp", "MCP 工具");

    private final String code;
    private final String label;

    public static AiToolSourceTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AiToolSourceTypeEnum item : values()) {
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
