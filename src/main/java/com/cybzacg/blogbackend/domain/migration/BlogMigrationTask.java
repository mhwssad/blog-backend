package com.cybzacg.blogbackend.domain.migration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部博客迁移任务。
 */
@Data
@TableName("blog_migration_task")
public class BlogMigrationTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourcePlatform;
    private String originalFileName;
    private String fileMd5;
    private Long fileSize;
    private String fileContentJson;
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
