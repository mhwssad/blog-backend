package com.cybzacg.blogbackend.module.article.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "前台文章分页查询条件")
public class PublicArticlePageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
    @Schema(description = "搜索关键词")
    private String keyword;
    @Schema(description = "分类ID")
    private Long categoryId;
    @Schema(description = "标签ID")
    private Long tagId;
    @Schema(description = "排序方式", example = "latest")
    private String sort = "latest";
}
