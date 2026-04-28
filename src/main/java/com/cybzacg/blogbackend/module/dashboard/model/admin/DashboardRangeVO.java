package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 看板指标时间范围。
 */
@Data
@Builder
@Schema(description = "看板指标时间范围")
public class DashboardRangeVO {
    @Schema(description = "时间范围：today/week/month/all/custom")
    private String rangeType;

    @Schema(description = "统计开始时间，all 时为空")
    private LocalDateTime startTime;

    @Schema(description = "统计结束时间，all 时为空")
    private LocalDateTime endTime;
}
