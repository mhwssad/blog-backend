package com.cybzacg.blogbackend.module.chat.governance.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建禁言请求。
 */
@Data
@Schema(description = "创建禁言请求")
public class ChatMuteCreateRequest {
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "被禁言用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "禁言范围不能为空")
    @Schema(description = "禁言范围：global/lobby/topic_channel/group", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scope;

    @Schema(description = "关联会话ID（lobby/topic_channel/group 时必填）")
    private Long conversationId;

    @Schema(description = "禁言截止时间（NULL 表示永久禁言）")
    private LocalDateTime muteUntil;

    @Schema(description = "禁言原因")
    private String reason;
}
