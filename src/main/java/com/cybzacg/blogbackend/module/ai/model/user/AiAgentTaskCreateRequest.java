package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户发起 Agent 任务请求。
 */
@Data
@Schema(description = "用户发起 Agent 任务请求")
public class AiAgentTaskCreateRequest {
    @NotNull(message = "Agent ID 不能为空")
    @Schema(description = "Agent 定义 ID")
    private Long agentId;

    @NotBlank(message = "输入内容不能为空")
    @Schema(description = "用户输入内容")
    private String inputContent;
}
