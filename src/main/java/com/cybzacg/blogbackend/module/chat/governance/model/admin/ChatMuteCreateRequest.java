package com.cybzacg.blogbackend.module.chat.governance.model.admin;

import com.cybzacg.blogbackend.core.validation.ConditionalNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建禁言请求。
 */
@Data
@Schema(description = "创建禁言请求")
@ConditionalNotBlank(field = "conversationId", dependsOn = "scope", values = {"topic_channel", "group"}, message = "该禁言范围必须指定关联会话ID")
public class ChatMuteCreateRequest {
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "被禁言用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotBlank(message = "禁言范围不能为空")
    @Pattern(regexp = "global|lobby|topic_channel|group", message = "禁言范围不合法")
    @Schema(description = "禁言范围：global/lobby/topic_channel/group", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scope;

    @Schema(description = "关联会话ID（lobby/topic_channel/group 时必填）")
    private Long conversationId;

    @Schema(description = "禁言截止时间（NULL 表示永久禁言）")
    private LocalDateTime muteUntil;

    @Size(max = 512, message = "禁言原因长度不能超过512")
    @Schema(description = "禁言原因")
    private String reason;
}
