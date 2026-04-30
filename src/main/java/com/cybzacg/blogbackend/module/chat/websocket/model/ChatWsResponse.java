package com.cybzacg.blogbackend.module.chat.websocket.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天 WebSocket 响应包。
 *
 * @param <T> 业务载荷类型
 */
@Data
@Builder
public class ChatWsResponse<T> {
    private String type;
    private String requestId;
    private LocalDateTime timestamp;
    private Integer code;
    private String message;
    private T payload;
}
