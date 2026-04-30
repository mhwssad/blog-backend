package com.cybzacg.blogbackend.module.chat.websocket.codec;

import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMessageType;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsReadyPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsRequest;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsResponse;
import com.cybzacg.blogbackend.utils.JsonUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天 WebSocket 消息编解码器。
 *
 * <p>统一处理协议包的序列化与基础响应构建，避免 Handler 直接拼装 JSON。
 */
@Component
public class ChatWebSocketMessageCodec {
    private static final List<String> SUPPORTED_REQUEST_TYPES = List.of(
            ChatWsMessageType.PING.getValue(),
            ChatWsMessageType.SEND_MESSAGE.getValue(),
            ChatWsMessageType.MARK_READ.getValue()
    );

    /**
     * 解析前端发送的文本消息为统一请求包。
     */
    public ChatWsRequest decode(String payload) {
        return JsonUtils.fromJson(payload, ChatWsRequest.class);
    }

    /**
     * 将请求包中的 payload 解码为指定对象。
     */
    public <T> T decodePayload(ChatWsRequest request, Class<T> payloadType) {
        if (request == null || request.getPayload() == null || request.getPayload().isNull()) {
            return null;
        }
        return JsonUtils.fromJson(request.getPayload().toString(), payloadType);
    }

    /**
     * 构造连接建立后的 ready 回包。
     */
    public TextMessage buildReady(WebSocketSession session) {
        ChatWsReadyPayload payload = ChatWsReadyPayload.builder()
                .sessionId(session.getId())
                .userId(resolveUserId(session))
                .username(resolveUsername(session))
                .supportedRequestTypes(SUPPORTED_REQUEST_TYPES)
                .build();
        return buildTextMessage(ChatWsResponse.builder()
                .type(ChatWsMessageType.READY.getValue())
                .timestamp(LocalDateTime.now())
                .code(ResultErrorCode.SUCCESS.getCode())
                .message(ResultErrorCode.SUCCESS.getMessage())
                .payload(payload)
                .build());
    }

    /**
     * 构造 ping 对应的 pong 回包。
     */
    public TextMessage buildPong(String requestId) {
        return buildTextMessage(baseResponse(ChatWsMessageType.PONG.getValue(), requestId, ResultErrorCode.SUCCESS, null, null));
    }

    /**
     * 构造成功确认回包。
     */
    public TextMessage buildAck(String requestId, Object payload) {
        return buildTextMessage(baseResponse(ChatWsMessageType.ACK.getValue(), requestId, ResultErrorCode.SUCCESS, null, payload));
    }

    /**
     * 构造服务端主动推送事件。
     */
    public TextMessage buildEvent(String type, Object payload) {
        return buildTextMessage(baseResponse(type, null, ResultErrorCode.SUCCESS, null, payload));
    }

    /**
     * 构造参数错误回包。
     */
    public TextMessage buildIllegalArgument(String requestId, String message) {
        return buildTextMessage(baseResponse(ChatWsMessageType.ERROR.getValue(), requestId, ResultErrorCode.ILLEGAL_ARGUMENT, message, null));
    }

    /**
     * 构造自定义业务错误回包。
     */
    public TextMessage buildBusinessError(String requestId, Integer code, String message) {
        return buildTextMessage(ChatWsResponse.builder()
                .type(ChatWsMessageType.ERROR.getValue())
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .code(code)
                .message(message)
                .payload(null)
                .build());
    }

    /**
     * 构造 JSON 解析失败回包。
     */
    public TextMessage buildJsonError(String requestId) {
        return buildTextMessage(baseResponse(ChatWsMessageType.ERROR.getValue(), requestId, ResultErrorCode.JSON_PROCESSING_ERROR, "WebSocket 消息不是合法 JSON", null));
    }

    /**
     * 构造未实现能力回包，提前占住协议类型。
     */
    public TextMessage buildUnsupported(String requestId, String type) {
        return buildTextMessage(baseResponse(
                ChatWsMessageType.ERROR.getValue(),
                requestId,
                ResultErrorCode.UNSUPPORTED_OPERATION,
                "当前消息类型暂未实现: " + type,
                null));
    }

    private ChatWsResponse<Object> baseResponse(String type,
                                                String requestId,
                                                ResultErrorCode resultErrorCode,
                                                String message,
                                                Object payload) {
        return ChatWsResponse.builder()
                .type(type)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .code(resultErrorCode.getCode())
                .message(message != null ? message : resultErrorCode.getMessage())
                .payload(payload)
                .build();
    }

    private TextMessage buildTextMessage(Object body) {
        return new TextMessage(JsonUtils.toJson(body));
    }

    private Long resolveUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(WebSocketConstants.ATTR_USER_ID);
        if (value instanceof Long userId) {
            return userId;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String resolveUsername(WebSocketSession session) {
        Object value = session.getAttributes().get(WebSocketConstants.ATTR_USERNAME);
        return value != null ? String.valueOf(value) : null;
    }
}
