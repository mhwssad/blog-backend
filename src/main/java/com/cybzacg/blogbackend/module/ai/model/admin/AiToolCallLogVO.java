package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具调用日志 VO。
 */
@Data
@Schema(description = "AI 工具调用日志信息")
public class AiToolCallLogVO {
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "用户 ID")
    private Long userId;
    @Schema(description = "Agent ID")
    private Long agentId;
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "任务 ID")
    private Long taskId;
    @Schema(description = "工具 ID")
    private Long toolId;
    @Schema(description = "工具编码")
    private String toolCode;
    @Schema(description = "工具名称")
    private String toolName;
    @Schema(description = "请求场景")
    private String requestSceneType;
    @Schema(description = "入参摘要")
    private String requestSummary;
    @Schema(description = "结果摘要")
    private String responseSummary;
    @Schema(description = "成功状态")
    private Integer successStatus;
    @Schema(description = "耗时毫秒")
    private Long elapsedMs;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
