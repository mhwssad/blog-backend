package com.cybzacg.blogbackend.module.article.model.user;

import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesArticleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧系列详情。
 */
@Data
@Schema(description = "用户侧系列详情")
public class UserArticleSeriesDetailVO {
    @Schema(description = "系列ID")
    private Long id;

    @Schema(description = "系列标题")
    private String title;

    @Schema(description = "系列描述")
    private String description;

    @Schema(description = "系列封面")
    private String coverImage;

    @Schema(description = "系列状态")
    private Integer status;

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
