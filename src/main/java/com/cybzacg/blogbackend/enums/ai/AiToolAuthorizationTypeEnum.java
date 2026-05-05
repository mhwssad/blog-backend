package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 工具授权类型。
 */
@Getter
@AllArgsConstructor
public enum AiToolAuthorizationTypeEnum {
    AGENT("agent", "按 Agent 授权"),
    SCENE("scene", "按场景授权"),
    PERMISSION("permission", "按权限授权"),
    DATA_SCOPE("data_scope", "按数据范围授权");

    private final String code;
    private final String label;

    public static AiToolAuthorizationTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AiToolAuthorizationTypeEnum item : values()) {
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
