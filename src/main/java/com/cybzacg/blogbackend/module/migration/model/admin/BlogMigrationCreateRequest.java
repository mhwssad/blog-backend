package com.cybzacg.blogbackend.module.migration.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 博客迁移任务创建请求。
 */
@Data
@Schema(description = "博客迁移任务创建请求")
public class BlogMigrationCreateRequest {
    @NotNull(message = "作者ID不能为空")
    @Schema(description = "导入文章归属作者ID")
    private Long authorId;

    @Size(max = 256, message = "备注长度不能超过256")
    @Schema(description = "备注")
    private String remark;
}
