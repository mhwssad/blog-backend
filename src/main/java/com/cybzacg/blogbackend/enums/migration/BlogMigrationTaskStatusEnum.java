package com.cybzacg.blogbackend.enums.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 博客迁移任务状态。
 */
@Getter
@AllArgsConstructor
public enum BlogMigrationTaskStatusEnum {
    CREATED(0, "已创建"),
    PRECHECKED(1, "预检通过"),
    RUNNING(2, "执行中"),
    COMPLETED(3, "已完成"),
    FAILED(4, "失败"),
    CANCELLED(5, "已取消");

    private final Integer value;
    private final String description;

    public static boolean contains(Integer value) {
        for (BlogMigrationTaskStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
