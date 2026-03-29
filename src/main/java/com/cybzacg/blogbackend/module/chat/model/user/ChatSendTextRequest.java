package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送文本消息请求。
 */
@Data
@Schema(description = "发送文本消息请求")
public class ChatSendTextRequest {
    @Schema(description = "会话ID；已存在会话时优先传该字段")
    private Long conversationId;

    @Schema(description = "单聊目标用户ID；未传会话ID时用于自动创建 / 获取单聊")
    private Long targetUserId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容长度不能超过2000")
    @Schema(description = "文本消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Size(max = 64, message = "客户端消息ID长度不能超过64")
    @Schema(description = "客户端幂等消息ID")
    private String clientMessageId;

    @Schema(description = "回复的消息ID")
    private Long replyMessageId;
}
