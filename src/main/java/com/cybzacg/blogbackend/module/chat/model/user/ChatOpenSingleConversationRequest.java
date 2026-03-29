package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单聊会话打开请求。
 */
@Data
@Schema(description = "单聊会话打开请求")
public class ChatOpenSingleConversationRequest {
    @NotNull(message = "目标用户不能为空")
    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetUserId;
}
