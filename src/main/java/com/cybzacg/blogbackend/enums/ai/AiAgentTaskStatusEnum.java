package com.cybzacg.blogbackend.enums.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI Agent 任务状态枚举。
 */
@Getter
@AllArgsConstructor
public enum AiAgentTaskStatusEnum {

    PENDING(0, "待执行"),
    RUNNING(1, "执行中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败"),
    CANCELLED(4, "已取消");

    private final Integer value;
    private final String label;

    /**
     * 根据 value 获取枚举值。
     */
    public static AiAgentTaskStatusEnum fromValue(Integer value) {
        for (AiAgentTaskStatusEnum e : values()) {
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
