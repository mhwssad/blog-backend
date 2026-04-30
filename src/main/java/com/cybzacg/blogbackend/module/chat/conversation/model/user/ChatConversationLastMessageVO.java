package com.cybzacg.blogbackend.module.chat.conversation.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话最后一条消息摘要。
 */
@Data
@Schema(description = "会话最后一条消息摘要")
public class ChatConversationLastMessageVO {
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "发送人ID")
    private Long senderId;

    @Schema(description = "发送人昵称")
    private String senderNickname;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "发送时间")
    private LocalDateTime createdAt;
}
