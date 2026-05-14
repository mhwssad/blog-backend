package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.validation.ValidDataScopeArray;
import com.cybzacg.blogbackend.core.validation.ValidJsonObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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

    @ValidDataScopeArray(message = "Agent 数据读取范围包含未知配置")
    @Schema(description = "数据读取范围配置 JSON")
    private String dataScopeJson;

    @Min(value = 1, message = "最大对话轮次必须大于 0")
    @Schema(description = "最大对话轮次，默认1")
    private Integer maxTurns;

    @ValidJsonObject(message = "扩展配置必须是 JSON 对象")
    @Schema(description = "扩展配置 JSON")
    private String extraConfigJson;
}
