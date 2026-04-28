package com.cybzacg.blogbackend.module.article.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章所属系列摘要。
 */
@Data
@Schema(description = "文章所属系列摘要")
public class ArticleSeriesSummaryVO {
    @Schema(description = "系列ID")
    private Long id;

    @Schema(description = "系列标题")
    private String title;

    @Schema(description = "系列封面")
    private String coverImage;

    @Schema(description = "系列文章数")
    private Integer articleCount;

    @Schema(description = "系列排序值")
    private Integer sortOrder;

    @Schema(description = "系列可见范围")
    private Integer visibilityScope;
}
