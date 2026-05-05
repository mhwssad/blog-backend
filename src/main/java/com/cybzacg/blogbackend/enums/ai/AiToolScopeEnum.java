package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 工具适用场景。
 */
@Getter
@AllArgsConstructor
public enum AiToolScopeEnum {
    AGENT("agent", "Agent 场景"),
    CHAT("chat", "对话场景"),
    ADMIN("admin", "后台场景"),
    SYSTEM("system", "系统场景");

    private final String code;
    private final String label;

    public static AiToolScopeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AiToolScopeEnum item : values()) {
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
