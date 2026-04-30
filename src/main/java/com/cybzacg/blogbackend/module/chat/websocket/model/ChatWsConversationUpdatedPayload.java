package com.cybzacg.blogbackend.module.chat.websocket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 会话级变更 WebSocket 事件载荷。
 */
@Data
@Builder
@Schema(description = "会话级变更 WebSocket 事件载荷")
public class ChatWsConversationUpdatedPayload {
    @Schema(description = "变更动作")
    private String action;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "会话类型")
    private String conversationType;

    @Schema(description = "会话名称")
    private String name;

    @Schema(description = "会话头像")
    private String avatar;

    @Schema(description = "群主ID")
    private Long ownerId;

    @Schema(description = "群公告")
    private String notice;

    @Schema(description = "会话状态")
    private Integer status;

    @Schema(description = "当前活跃成员数")
    private Long memberCount;
}
