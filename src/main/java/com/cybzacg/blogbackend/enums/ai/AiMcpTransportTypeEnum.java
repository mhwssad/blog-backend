package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP 传输类型。
 */
@Getter
@AllArgsConstructor
public enum AiMcpTransportTypeEnum {
    STDIO("stdio", "标准输入输出"),
    HTTP("http", "Streamable HTTP");

    private final String code;
    private final String label;

    public static AiMcpTransportTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AiMcpTransportTypeEnum item : values()) {
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
