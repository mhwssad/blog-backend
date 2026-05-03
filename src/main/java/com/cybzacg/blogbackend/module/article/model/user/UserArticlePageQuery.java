package com.cybzacg.blogbackend.module.article.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户侧文章分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户侧文章分页查询条件")
public class UserArticlePageQuery extends PageQuery {

    @Schema(description = "搜索关键字")
    private String keyword;

    @Schema(description = "文章状态")
    private Integer status;

    @Schema(description = "审核状态")
    private Integer reviewStatus;

    @Schema(description = "可见范围")
    private Integer visibilityScope;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签ID")
    private Long tagId;
}
