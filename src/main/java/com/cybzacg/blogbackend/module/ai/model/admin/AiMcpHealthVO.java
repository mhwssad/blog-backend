package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * MCP 健康检查结果。
 */
@Data
@Schema(description = "MCP 健康检查结果")
public class AiMcpHealthVO {
    @Schema(description = "是否健康")
    private Boolean healthy;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "错误摘要")
    private String errorSummary;
}
