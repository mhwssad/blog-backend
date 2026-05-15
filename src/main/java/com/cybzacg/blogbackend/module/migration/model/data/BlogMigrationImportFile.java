package com.cybzacg.blogbackend.module.migration.model.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 博客迁移 JSON 文件。
 */
@Data
public class BlogMigrationImportFile {
    @NotBlank(message = "来源平台不能为空")
    private String sourcePlatform;
    @NotEmpty(message = "迁移文件文章列表不能为空")
    @Valid
    private List<BlogMigrationPostItem> posts;
}
