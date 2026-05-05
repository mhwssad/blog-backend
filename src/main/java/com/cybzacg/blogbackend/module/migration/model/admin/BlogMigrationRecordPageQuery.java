package com.cybzacg.blogbackend.module.migration.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 博客迁移文章记录分页查询。
 */
@Data
@Schema(description = "博客迁移文章记录分页查询")
public class BlogMigrationRecordPageQuery extends PageQuery {
    @Schema(description = "任务ID")
    private Long taskId;
    @Schema(description = "记录状态")
    private Integer status;
}
