package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.validation.ValidDataScope;
import com.cybzacg.blogbackend.core.validation.ValidJsonObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 工具执行请求。
 */
@Data
@Schema(description = "AI 工具执行请求")
public class AiToolExecuteRequest {
    @NotBlank(message = "工具编码不能为空")
    @Schema(description = "工具编码")
    private String toolCode;
    @NotBlank(message = "工具参数不能为空")
    @ValidJsonObject(message = "工具参数必须是 JSON 对象")
    @Schema(description = "工具参数 JSON")
    private String arguments;
    @Schema(description = "Agent ID")
    private Long agentId;
    @Schema(description = "会话 ID")
    private Long sessionId;
    @Schema(description = "任务 ID")
    private Long taskId;
    @Size(max = 32, message = "场景类型最多32个字符")
    @Schema(description = "场景类型")
    private String sceneType;
    @ValidDataScope
    @Schema(description = "数据范围")
    private String dataScope;
}
