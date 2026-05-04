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
    @Schema(description = "时间范围")
    private DashboardRangeVO range;

    @Schema(description = "AI 调用次数")
    private Long aiCallCount;

    @Schema(description = "AI 成功调用次数")
    private Long aiSuccessCallCount;

    @Schema(description = "AI 失败调用次数")
    private Long aiFailedCallCount;

    @Schema(description = "RAG 调用次数")
    private Long ragCallCount;

    @Schema(description = "Agent 任务总数")
    private Long agentTaskCount;

    @Schema(description = "Agent 成功任务数")
    private Long agentSuccessTaskCount;

    @Schema(description = "Agent 失败任务数")
    private Long agentFailedTaskCount;
}
