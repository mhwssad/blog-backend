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
    @Schema(description = "时间范围")
    private DashboardRangeVO range;

    @Schema(description = "注册用户数")
    private Long registeredUserCount;

    @Schema(description = "活跃用户数")
    private Long activeUserCount;

    @Schema(description = "作者数量")
    private Long authorCount;

    @Schema(description = "文章总数")
    private Long articleCount;

    @Schema(description = "待审核文章数")
    private Long pendingArticleReviewCount;

    @Schema(description = "评论数")
    private Long commentCount;

    @Schema(description = "私信消息数")
    private Long chatMessageCount;

    @Schema(description = "AI 调用次数")
    private Long aiCallCount;

    @Schema(description = "举报总数")
    private Long reportCount;

    @Schema(description = "待处理举报数")
    private Long pendingReportCount;
}
