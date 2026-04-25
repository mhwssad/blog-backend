package com.cybzacg.blogbackend.module.chat.model.websocket;

import com.cybzacg.blogbackend.module.chat.model.user.ChatMemberVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 成员变更 WebSocket 事件载荷。
 */
@Data
@Builder
@Schema(description = "成员变更 WebSocket 事件载荷")
public class ChatWsMembersUpdatedPayload {
    @Schema(description = "变更动作")
    private String action;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "受影响成员ID")
    private Long affectedUserId;

    @Schema(description = "当前活跃成员列表")
    private List<ChatMemberVO> members;
}
