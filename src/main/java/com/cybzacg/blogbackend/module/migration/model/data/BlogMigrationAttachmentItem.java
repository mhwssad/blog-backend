package com.cybzacg.blogbackend.module.migration.model.data;

import lombok.Data;

/**
 * 博客迁移附件数据项。
 */
@Data
public class BlogMigrationAttachmentItem {
    private String url;
    private String originalName;
}
