package com.cybzacg.blogbackend.module.forum.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台论坛版块分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台论坛版块分页查询参数")
public class ForumSectionPageQuery extends PageQuery {
    @Schema(description = "版块名称或简介关键字")
    private String keyword;

    @Min(value = 0, message = "版块状态不合法")
    @Max(value = 1, message = "版块状态不合法")
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Min(value = 0, message = "版块可见范围不合法")
    @Max(value = 1, message = "版块可见范围不合法")
    @Schema(description = "可见范围：0-公开，1-登录可见")
    private Integer visibilityScope;
}
