package com.cybzacg.blogbackend.module.chat.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回复消息快照。
 */
@Data
@Schema(description = "回复消息快照")
public class ChatReplyMessageVO {
    @Schema(description = "被回复消息ID")
    private Long id;

    @Schema(description = "被回复消息发送人ID")
    private Long senderId;

    @Schema(description = "被回复消息发送人用户名")
    private String senderUsername;

    @Schema(description = "被回复消息发送人昵称")
    private String senderNickname;

    @Schema(description = "被回复消息发送人头像")
    private String senderAvatar;

    @Schema(description = "被回复消息类型")
    private String messageType;

    @Schema(description = "被回复消息自身所引用的上一层消息ID，仅用于前端状态链接，不继续内联多层快照")
    private Long replyToMessageId;

    @Schema(description = "被回复消息摘要内容")
    private String content;

    @Schema(description = "被回复消息附件快照")
    private ChatFilePayloadVO file;

    @Schema(description = "被回复消息是否已撤回")
    private Boolean revoked;

    @Schema(description = "被回复消息当前是否已不可见，仅用于缺失回退提示")
    private Boolean deleted;

    @Schema(description = "被回复消息当前状态：normal/revoked/unavailable")
    private String state;

    @Schema(description = "被回复消息发送时间")
    private LocalDateTime createdAt;
}
