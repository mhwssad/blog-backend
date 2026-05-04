package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Agent 定义创建/更新请求。
 */
@Data
@Schema(description = "Agent 定义创建/更新请求")
public class AiAgentDefinitionSaveRequest {
    @NotBlank(message = "Agent 名称不能为空")
    @Size(max = 64, message = "Agent 名称最多64个字符")
    @Schema(description = "Agent 名称")
    private String name;

    @Size(max = 512, message = "Agent 描述最多512个字符")
    @Schema(description = "Agent 描述")
    private String description;

    @NotBlank(message = "系统提示词不能为空")
    @Schema(description = "系统提示词")
    private String systemPrompt;

    @NotNull(message = "渠道配置 ID 不能为空")
    @Schema(description = "关联 AI 渠道配置 ID")
    private Long channelConfigId;

    @Schema(description = "数据读取范围配置 JSON")
    private String dataScopeJson;

    @Schema(description = "最大对话轮次，默认1")
    private Integer maxTurns;

    @Schema(description = "扩展配置 JSON")
    private String extraConfigJson;
}
