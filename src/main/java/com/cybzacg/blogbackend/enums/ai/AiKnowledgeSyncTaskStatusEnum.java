package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 知识同步任务状态枚举。
 */
@Getter
@AllArgsConstructor
public enum AiKnowledgeSyncTaskStatusEnum {

    PENDING(0, "待执行"),
    RUNNING(1, "执行中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败");

    private final Integer value;
    private final String label;

    /**
     * 根据 value 获取枚举值。
     */
    public static AiKnowledgeSyncTaskStatusEnum fromValue(Integer value) {
        for (AiKnowledgeSyncTaskStatusEnum e : values()) {
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
