package com.cybzacg.blogbackend.module.migration.model.data;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 博客迁移附件数据项。
 */
@Data
public class BlogMigrationAttachmentItem {
    @NotBlank(message = "附件URL不能为空")
    private String url;
    private String originalName;
}
