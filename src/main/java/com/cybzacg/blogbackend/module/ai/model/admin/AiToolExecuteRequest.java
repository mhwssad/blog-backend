package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 工具执行请求。
 */
@Data
@Schema(description = "AI 工具执行请求")
public class AiToolExecuteRequest {
    @Schema(description = "工具编码")
    private String toolCode;
    @Schema(description = "工具参数 JSON")
    private String arguments;
    @Schema(description = "Agent ID")
    private Long agentId;
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "任务 ID")
    private Long taskId;
    @Schema(description = "场景类型")
    private String sceneType;
    @Schema(description = "数据范围")
    private String dataScope;
}
