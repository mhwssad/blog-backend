package com.cybzacg.blogbackend.module.chat.websocket.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 聊天 WebSocket 请求包。
 */
@Data
public class ChatWsRequest {
    private String type;
    private String requestId;
    private JsonNode payload;
}
