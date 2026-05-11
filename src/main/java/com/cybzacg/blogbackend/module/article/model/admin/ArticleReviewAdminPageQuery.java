package com.cybzacg.blogbackend.module.article.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台文章审核分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台文章审核分页查询条件")
public class ArticleReviewAdminPageQuery extends PageQuery {
    @Schema(description = "搜索关键")
    private String keyword;
    @Schema(description = "作者ID")
    private Long authorId;
    @Schema(description = "审核状态，默认 1=审核")
    private Integer reviewStatus;
}
