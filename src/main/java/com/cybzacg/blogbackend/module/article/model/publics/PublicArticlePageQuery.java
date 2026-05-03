package com.cybzacg.blogbackend.module.article.model.publics;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "前台文章分页查询条件")
public class PublicArticlePageQuery extends PageQuery {
    @Schema(description = "搜索关键")
    private String keyword;
    @Schema(description = "分类ID")
    private Long categoryId;
    @Schema(description = "标签ID")
    private Long tagId;
    @Schema(description = "排序方式", example = "latest")
    private String sort = "latest";
}
