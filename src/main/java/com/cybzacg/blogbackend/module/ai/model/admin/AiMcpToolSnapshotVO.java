package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 工具快照 VO。
 */
@Data
@Schema(description = "MCP 工具快照信息")
public class AiMcpToolSnapshotVO {
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "MCP 服务 ID")
    private Long mcpServerId;
    @Schema(description = "MCP 原始工具名")
    private String mcpToolName;
    @Schema(description = "工具编码")
    private String toolCode;
    @Schema(description = "工具名称")
    private String toolName;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "参数 Schema")
    private String parametersSchema;
    @Schema(description = "返回 Schema")
    private String resultSchema;
    @Schema(description = "风险等级")
    private String riskLevel;
    @Schema(description = "适用场景")
    private String useScenarios;
    @Schema(description = "启用状态")
    private Integer enabled;
    @Schema(description = "发现时间")
    private LocalDateTime discoveredAt;
    @Schema(description = "原始定义 JSON")
    private String rawDefinitionJson;
    @Schema(description = "错误摘要")
    private String lastErrorSummary;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
