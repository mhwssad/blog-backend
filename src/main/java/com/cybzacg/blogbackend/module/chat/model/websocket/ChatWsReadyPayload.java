package com.cybzacg.blogbackend.module.chat.model.websocket;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * WebSocket 握手完成后的 ready 载荷。
 */
@Data
@Builder
public class ChatWsReadyPayload {
    private String sessionId;
    private Long userId;
    private String username;
    private List<String> supportedRequestTypes;
}
