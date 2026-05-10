package com.cybzacg.blogbackend.dto.domain.migration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部博客迁移文章记录。
 */
@Data
@TableName("blog_migration_record")
public class BlogMigrationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String sourcePlatform;
    private String externalPostId;
    private String idempotentKey;
    private String originalTitle;
    private Integer status;
    private Long targetArticleId;
    private String errorMessage;
    private String rawContentJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
