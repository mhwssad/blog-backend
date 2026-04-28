package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI使用统计信息。
 */
@Data
@Schema(description = "AI使用统计信息")
public class AiUsageStatsVO {
    @Schema(description = "总调用次数")
    private long totalCalls;

    @Schema(description = "成功调用次数")
    private long successCalls;

    @Schema(description = "失败调用次数")
    private long failedCalls;

    @Schema(description = "总token数")
    private long totalTokens;

    @Schema(description = "总额度消耗")
    private long totalQuotaCost;
}
