package com.cybzacg.blogbackend.module.chat.model.websocket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 消息删除事件载荷。
 */
@Data
@Builder
@Schema(description = "消息删除事件载荷")
public class ChatWsMessageDeletedPayload {
    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "执行删除的用户ID")
    private Long userId;

    @Schema(description = "删除后的当前会话未读数")
    private Integer unreadCount;
}
