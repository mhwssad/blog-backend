package com.cybzacg.blogbackend.module.migration.model.data;

import lombok.Data;

import java.util.List;

/**
 * 博客迁移 JSON 文件。
 */
@Data
public class BlogMigrationImportFile {
    private String sourcePlatform;
    private List<BlogMigrationPostItem> posts;
}
