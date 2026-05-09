package com.cybzacg.blogbackend.module.migration.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客迁移任务分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "博客迁移任务分页查询")
public class BlogMigrationTaskPageQuery extends PageQuery {
    @Schema(description = "任务状态")
    private Integer status;
    @Schema(description = "来源平台")
    private String sourcePlatform;
    @Schema(description = "作者ID")
    private Long authorId;
}
