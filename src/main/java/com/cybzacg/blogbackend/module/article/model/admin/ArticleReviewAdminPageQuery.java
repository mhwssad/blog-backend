package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台文章审核分页查询条件。
 */
@Data
@Schema(description = "后台文章审核分页查询条件")
public class ArticleReviewAdminPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;
    @Schema(description = "每页条数")
    private Long size = 10L;
    @Schema(description = "搜索关键词")
    private String keyword;
    @Schema(description = "作者ID")
    private Long authorId;
    @Schema(description = "审核状态，默认 1=审核中")
    private Integer reviewStatus;
}
