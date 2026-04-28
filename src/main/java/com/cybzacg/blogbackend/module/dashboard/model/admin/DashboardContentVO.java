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
    private DashboardRangeVO range;
    private Long articleCount;
    private Long pendingArticleReviewCount;
    private Long commentCount;
    private Long likeCount;
    private Long collectCount;
}
