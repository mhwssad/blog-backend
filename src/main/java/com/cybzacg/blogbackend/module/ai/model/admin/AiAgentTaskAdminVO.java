package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 任务后台管理响应 VO。
 */
@Data
@Schema(description = "Agent 任务管理信息")
public class AiAgentTaskAdminVO {
    @Schema(description = "任务ID")
    private Long id;
    @Schema(description = "发起用户ID")
    private Long userId;
    @Schema(description = "关联 Agent 定义 ID")
    private Long agentId;
    @Schema(description = "Agent 名称")
    private String agentName;
    @Schema(description = "状态：0-待执行 1-执行中 2-已完成 3-失败 4-已取消")
    private Integer status;
    @Schema(description = "用户输入")
    private String inputContent;
    @Schema(description = "Agent 输出")
    private String outputContent;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "消耗 token 数")
    private Integer tokenCount;
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
