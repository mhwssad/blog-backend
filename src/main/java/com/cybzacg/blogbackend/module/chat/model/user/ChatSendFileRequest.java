package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送文件消息请求。
 */
@Data
@Schema(description = "发送文件消息请求")
public class ChatSendFileRequest {
    @Schema(description = "会话ID；已存在会话时优先传该字段")
    private Long conversationId;

    @Schema(description = "单聊目标用户ID；未传会话ID时用于自动创建 / 获取单聊")
    private Long targetUserId;

    @NotNull(message = "文件业务引用ID不能为空")
    @Schema(description = "上传完成后得到的文件业务引用ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long businessId;

    @Size(max = 64, message = "客户端消息ID长度不能超过64")
    @Schema(description = "客户端幂等消息ID")
    private String clientMessageId;

    @Schema(description = "回复的消息ID")
    private Long replyMessageId;
}
