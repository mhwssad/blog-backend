package com.cybzacg.blogbackend.module.chat.model.websocket;

import lombok.Data;

/**
 * WebSocket 已读推进请求载荷。
 */
@Data
public class ChatWsMarkReadPayload {
    private Long conversationId;
    private Long readMessageId;
}
