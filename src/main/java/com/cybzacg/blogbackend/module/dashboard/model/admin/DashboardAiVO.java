package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 后台 AI 统计指标。
 */
@Data
@Builder
@Schema(description = "后台 AI 统计指标")
public class DashboardAiVO {
    private DashboardRangeVO range;
    private Long aiCallCount;
    private Long aiSuccessCallCount;
    private Long aiFailedCallCount;
}
