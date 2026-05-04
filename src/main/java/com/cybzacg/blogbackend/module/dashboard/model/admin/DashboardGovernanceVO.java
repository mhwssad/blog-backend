package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 后台治理统计指标。
 */
@Data
@Builder
@Schema(description = "后台治理统计指标")
public class DashboardGovernanceVO {
    @Schema(description = "时间范围")
    private DashboardRangeVO range;

    @Schema(description = "举报总数")
    private Long reportCount;

    @Schema(description = "待处理举报数")
    private Long pendingReportCount;

    @Schema(description = "处理中举报数")
    private Long processingReportCount;

    @Schema(description = "已处理举报数")
    private Long handledReportCount;

    @Schema(description = "已驳回举报数")
    private Long rejectedReportCount;

    @Schema(description = "平均举报处理耗时（分钟）")
    private BigDecimal averageHandleDurationMinutes;

    @Schema(description = "举报处罚类型分布")
    private List<DashboardPunishmentDistributionVO> punishmentDistributions;
}
