package com.cybzacg.blogbackend.module.forum.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "可见范围：0-公开，1-登录可见")
    private Integer visibilityScope;
}
