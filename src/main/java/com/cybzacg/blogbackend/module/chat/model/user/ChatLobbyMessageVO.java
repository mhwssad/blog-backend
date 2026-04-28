package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "大厅频道消息（访客视图）")
public class ChatLobbyMessageVO {
    @Schema(description = "消息ID")
    private Long id;
    @Schema(description = "发送者ID")
    private Long senderId;
    @Schema(description = "发送者名称")
    private String senderName;
    @Schema(description = "发送者头像")
    private String senderAvatar;
    @Schema(description = "消息类型")
    private String messageType;
    @Schema(description = "消息内容")
    private String content;
    @Schema(description = "发送时间")
    private LocalDateTime createdAt;
}
