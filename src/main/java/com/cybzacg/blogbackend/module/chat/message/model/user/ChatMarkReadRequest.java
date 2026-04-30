package com.cybzacg.blogbackend.module.chat.message.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会话已读推进请求。
 */
@Data
@Schema(description = "会话已读推进请求")
public class ChatMarkReadRequest {
    @NotNull(message = "已读消息ID不能为空")
    @Schema(description = "最后已读消息ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long readMessageId;
}
