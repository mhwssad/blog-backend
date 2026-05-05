package com.cybzacg.blogbackend.module.migration.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客迁移任务响应。
 */
@Data
@Schema(description = "博客迁移任务响应")
public class BlogMigrationTaskVO {
    private Long id;
    private String sourcePlatform;
    private String originalFileName;
    private String fileMd5;
    private Long fileSize;
    private Long authorId;
    private Integer status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer skipCount;
    private String errorSummary;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime precheckedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
