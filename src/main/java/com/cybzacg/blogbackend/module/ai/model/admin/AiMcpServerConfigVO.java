package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 服务配置 VO。
 */
@Data
@Schema(description = "MCP 服务配置信息")
public class AiMcpServerConfigVO {
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "服务名称")
    private String serverName;
    @Schema(description = "传输类型")
    private String transportType;
    @Schema(description = "连接配置 JSON")
    private String connectionConfigJson;
    @Schema(description = "超时时间（秒）")
    private Integer timeoutSeconds;
    @Schema(description = "启用状态")
    private Integer enabled;
    @Schema(description = "最近健康状态")
    private String lastHealthStatus;
    @Schema(description = "最近发现时间")
    private LocalDateTime lastDiscoveredAt;
    @Schema(description = "最近错误摘要")
    private String lastErrorSummary;
    @Schema(description = "创建人ID")
    private Long createdBy;
    @Schema(description = "更新人ID")
    private Long updatedBy;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
