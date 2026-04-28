package com.cybzacg.blogbackend.module.article.model.publics;

import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesArticleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开系列详情。
 */
@Data
@Schema(description = "公开系列详情")
public class PublicArticleSeriesDetailVO {
    @Schema(description = "系列ID")
    private Long id;

    @Schema(description = "系列标题")
    private String title;

    @Schema(description = "系列描述")
    private String description;

    @Schema(description = "系列封面")
    private String coverImage;

    @Schema(description = "创建人ID")
    private Long ownerUserId;

    @Schema(description = "创建人名称")
    private String ownerName;

    @Schema(description = "可见范围")
    private Integer visibilityScope;

    @Schema(description = "系列文章数")
    private Integer articleCount;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "系列文章列表")
    private List<ArticleSeriesArticleVO> articles;
}
