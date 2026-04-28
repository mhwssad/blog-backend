package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 后台治理统计指标。
 */
@Data
@Builder
@Schema(description = "后台治理统计指标")
public class DashboardGovernanceVO {
    private DashboardRangeVO range;
    private Long reportCount;
    private Long pendingReportCount;
    private Long processingReportCount;
    private Long handledReportCount;
    private Long rejectedReportCount;
}
