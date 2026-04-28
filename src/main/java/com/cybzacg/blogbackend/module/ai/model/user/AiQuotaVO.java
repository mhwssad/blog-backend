package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI配额信息。
 */
@Data
@Schema(description = "AI配额信息")
public class AiQuotaVO {
    @Schema(description = "每日限额")
    private int dailyLimit;

    @Schema(description = "今日已用")
    private long usedToday;

    @Schema(description = "今日剩余")
    private long remainingToday;
}
