package com.cybzacg.blogbackend.module.article.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系列内文章展示对象。
 */
@Data
@Schema(description = "系列内文章项")
public class ArticleSeriesArticleVO {
    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章封面")
    private String coverImage;

    @Schema(description = "文章状态")
    private Integer status;

    @Schema(description = "审核状态")
    private Integer reviewStatus;

    @Schema(description = "文章可见范围")
    private Integer visibilityScope;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "系列内顺序")
    private Integer seqNo;
}
