package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 定义响应 VO。
 */
@Data
@Schema(description = "Agent 定义信息")
public class AiAgentDefinitionVO {
    @Schema(description = "Agent ID")
    private Long id;
    @Schema(description = "Agent 名称")
    private String name;
    @Schema(description = "Agent 描述")
    private String description;
    @Schema(description = "系统提示词")
    private String systemPrompt;
    @Schema(description = "关联 AI 渠道配置 ID")
    private Long channelConfigId;
    @Schema(description = "数据读取范围配置 JSON")
    private String dataScopeJson;
    @Schema(description = "0-停用，1-启用")
    private Integer enabled;
    @Schema(description = "最大对话轮次")
    private Integer maxTurns;
    @Schema(description = "扩展配置 JSON")
    private String extraConfigJson;
    @Schema(description = "创建人ID")
    private Long createdBy;
    @Schema(description = "更新人ID")
    private Long updatedBy;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
