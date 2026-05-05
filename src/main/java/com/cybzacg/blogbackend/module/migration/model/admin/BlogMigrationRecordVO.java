package com.cybzacg.blogbackend.module.migration.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客迁移文章记录响应。
 */
@Data
@Schema(description = "博客迁移文章记录响应")
public class BlogMigrationRecordVO {
    private Long id;
    private Long taskId;
    private String sourcePlatform;
    private String externalPostId;
    private String idempotentKey;
    private String originalTitle;
    private Integer status;
    private Long targetArticleId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
