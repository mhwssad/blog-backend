package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具定义 VO。
 */
@Data
@Schema(description = "AI 工具定义信息")
public class AiToolDefinitionVO {
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "工具编码")
    private String toolCode;
    @Schema(description = "工具名称")
    private String toolName;
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "MCP 服务 ID")
    private Long mcpServerId;
    @Schema(description = "MCP 原始工具名")
    private String mcpToolName;
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
    @Schema(description = "创建人ID")
    private Long createdBy;
    @Schema(description = "更新人ID")
    private Long updatedBy;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
