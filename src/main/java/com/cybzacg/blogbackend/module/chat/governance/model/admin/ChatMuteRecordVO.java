package com.cybzacg.blogbackend.module.chat.governance.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 禁言记录视图。
 */
@Data
@Schema(description = "禁言记录视图")
public class ChatMuteRecordVO {
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "被禁言用户ID")
    private Long userId;

    @Schema(description = "被禁言用户名")
    private String username;

    @Schema(description = "被禁言用户昵称")
    private String nickname;

    @Schema(description = "禁言范围：global/lobby/topic_channel/group")
    private String scope;

    @Schema(description = "关联会话ID")
    private Long conversationId;

    @Schema(description = "关联会话名称")
    private String conversationName;

    @Schema(description = "禁言截止时间（NULL 表示永久）")
    private LocalDateTime muteUntil;

    @Schema(description = "状态：0-已解除 1-生效中")
    private Integer status;

    @Schema(description = "禁言原因")
    private String reason;

    @Schema(description = "来源：admin/report/auto")
    private String sourceType;

    @Schema(description = "关联举报ID")
    private Long reportId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人用户名")
    private String operatorUsername;

    @Schema(description = "解除人ID")
    private Long releasedBy;

    @Schema(description = "解除时间")
    private LocalDateTime releasedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
