package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 知识条目状态枚举。
 */
@Getter
@AllArgsConstructor
public enum AiKnowledgeEntryStatusEnum {

    DISABLED(0, "禁用"),
    ACTIVE(1, "正常"),
    OUTDATED(2, "过期"),
    DELETED(3, "已删除");

    private final Integer value;
    private final String label;

    /**
     * 根据 value 获取枚举值。
     */
    public static AiKnowledgeEntryStatusEnum fromValue(Integer value) {
        for (AiKnowledgeEntryStatusEnum e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 判断 value 是否为有效枚举值。
     */
    public static boolean contains(Integer value) {
        return fromValue(value) != null;
    }
}
