package com.cybzacg.blogbackend.module.chat.model.websocket;

import lombok.Builder;
import lombok.Data;

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
    private Long timestamp;
    private Integer code;
    private String message;
    private T payload;
}
