package com.cybzacg.blogbackend.domain.migration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部博客迁移附件记录。
 */
@Data
@TableName("blog_migration_attachment")
public class BlogMigrationAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long recordId;
    private String externalUrl;
    private String originalName;
    private Integer status;
    private Long fileId;
    private String fileUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
