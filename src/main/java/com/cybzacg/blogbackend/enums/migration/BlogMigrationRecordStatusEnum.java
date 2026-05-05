package com.cybzacg.blogbackend.enums.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 博客迁移文章记录状态。
 */
@Getter
@AllArgsConstructor
public enum BlogMigrationRecordStatusEnum {
    PENDING(0, "待处理"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败"),
    SKIPPED(3, "已跳过");

    private final Integer value;
    private final String description;
}
