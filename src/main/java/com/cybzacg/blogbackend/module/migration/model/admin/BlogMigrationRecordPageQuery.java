package com.cybzacg.blogbackend.module.migration.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.core.web.PageQuery;
import com.cybzacg.blogbackend.enums.migration.BlogMigrationRecordStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客迁移文章记录分页查询。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "博客迁移文章记录分页查询")
public class BlogMigrationRecordPageQuery extends PageQuery {
    @Schema(description = "任务ID")
    private Long taskId;
    @EnumValue(enumClass = BlogMigrationRecordStatusEnum.class, message = "记录状态值非法")
    @Schema(description = "记录状态")
    private Integer status;
}
