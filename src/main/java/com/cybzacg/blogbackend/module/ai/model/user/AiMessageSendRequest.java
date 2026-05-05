package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    @Size(max = 5, message = "单条消息最多5个附件")
    @Schema(description = "附件文件ID列表（图片/文件）")
    private List<Long> attachmentFileIds;
}
