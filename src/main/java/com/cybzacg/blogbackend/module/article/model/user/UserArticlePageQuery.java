package com.cybzacg.blogbackend.module.article.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户侧文章分页查询条件。
 */
@Data
@Schema(description = "用户侧文章分页查询条件")
public class UserArticlePageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    @Schema(description = "搜索关键词")
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
