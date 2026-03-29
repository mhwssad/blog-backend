package com.cybzacg.blogbackend.module.chat.model.websocket;

import lombok.Builder;
import lombok.Data;

/**
 * WebSocket 指令确认载荷。
 */
@Data
@Builder
public class ChatWsAckPayload {
    private String requestType;
    private Object data;
}
