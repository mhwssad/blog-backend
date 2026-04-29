package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI消息发送请求。
 */
@Data
@Schema(description = "AI消息发送请求")
public class AiMessageSendRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字符")
    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "请求场景类型", example = "general")
    private String requestSceneType = "general";

    @Schema(description = "关联目标ID")
    private Long requestTargetId;
}
