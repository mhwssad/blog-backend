package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 工具定义创建/更新请求。
 */
@Data
@Schema(description = "AI 工具定义创建/更新请求")
public class AiToolDefinitionSaveRequest {
    @NotBlank(message = "工具编码不能为空")
    @Size(max = 64, message = "工具编码最多64个字符")
    @Schema(description = "工具编码")
    private String toolCode;

    @NotBlank(message = "工具名称不能为空")
    @Size(max = 128, message = "工具名称最多128个字符")
    @Schema(description = "工具名称")
    private String toolName;

    @NotBlank(message = "来源类型不能为空")
    @Schema(description = "来源类型 builtin/mcp")
    private String sourceType;

    @Schema(description = "MCP 服务 ID")
    private Long mcpServerId;

    @Schema(description = "MCP 原始工具名")
    private String mcpToolName;

    @Schema(description = "工具描述")
    private String description;

    @Schema(description = "参数 Schema JSON")
    private String parametersSchema;

    @Schema(description = "返回 Schema JSON")
    private String resultSchema;

    @NotBlank(message = "风险等级不能为空")
    @Schema(description = "风险等级 low/medium/high")
    private String riskLevel;

    @Schema(description = "适用场景 JSON 数组")
    private String useScenarios;

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "启用状态：0-停用/1-启用")
    private Integer enabled;

    @Schema(description = "MFA 票据")
    private String mfaTicket;
}
