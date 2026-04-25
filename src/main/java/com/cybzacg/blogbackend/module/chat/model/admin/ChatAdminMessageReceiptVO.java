package com.cybzacg.blogbackend.module.chat.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台消息回执信息。
 */
@Data
@Schema(description = "后台消息回执信息")
public class ChatAdminMessageReceiptVO {
    @Schema(description = "接收记录ID")
    private Long id;

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "接收人ID")
    private Long recipientUserId;

    @Schema(description = "接收人用户名")
    private String recipientUsername;

    @Schema(description = "接收人昵称")
    private String recipientNickname;

    @Schema(description = "接收人头像")
    private String recipientAvatar;

    @Schema(description = "接收类型")
    private String receiveType;

    @Schema(description = "投递状态")
    private Integer deliveryStatus;

    @Schema(description = "送达时间")
    private LocalDateTime deliveredAt;

    @Schema(description = "已读时间")
    private LocalDateTime readAt;

    @Schema(description = "可见状态")
    private Integer visibleStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
