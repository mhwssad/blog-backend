package com.cybzacg.blogbackend.module.chat.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 后台会话状态更新请求。
 */
@Data
@Schema(description = "后台会话状态更新请求")
public class ChatConversationStatusUpdateRequest {
    @NotNull(message = "会话状态不能为空")
    @Schema(description = "会话状态：0-禁用，1-正常", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
