package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 后台内容统计指标。
 */
@Data
@Builder
@Schema(description = "后台内容统计指标")
public class DashboardContentVO {
    @Schema(description = "时间范围")
    private DashboardRangeVO range;

    @Schema(description = "文章总数")
    private Long articleCount;

    @Schema(description = "待审核文章数")
    private Long pendingArticleReviewCount;

    @Schema(description = "评论数")
    private Long commentCount;

    @Schema(description = "点赞数")
    private Long likeCount;

    @Schema(description = "收藏数")
    private Long collectCount;
}
