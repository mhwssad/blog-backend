package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 后台核心概览指标。
 */
@Data
@Builder
@Schema(description = "后台核心概览指标")
public class DashboardOverviewVO {
    private DashboardRangeVO range;
    private Long registeredUserCount;
    private Long activeUserCount;
    private Long authorCount;
    private Long articleCount;
    private Long pendingArticleReviewCount;
    private Long commentCount;
    private Long chatMessageCount;
    private Long aiCallCount;
    private Long reportCount;
    private Long pendingReportCount;
}
