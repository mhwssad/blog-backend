package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MCP 工具发现结果。
 */
@Data
@Schema(description = "MCP 工具发现结果")
public class AiMcpDiscoverResultVO {
    @Schema(description = "发现工具数量")
    private Integer discoveredCount;
    @Schema(description = "同步后的工具数量")
    private Integer syncedCount;
}
