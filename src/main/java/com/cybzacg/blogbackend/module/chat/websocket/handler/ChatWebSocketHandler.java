package com.cybzacg.blogbackend.module.chat.websocket.handler;

import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsAckPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMarkReadPayload;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsMessageType;
import com.cybzacg.blogbackend.module.chat.websocket.model.ChatWsRequest;
import com.cybzacg.blogbackend.module.chat.member.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.message.service.UserChatService;
import com.cybzacg.blogbackend.module.chat.websocket.codec.ChatWebSocketMessageCodec;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 聊天 WebSocket 处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ChatWebSocketMessageCodec messageCodec;
    private final UserChatService userChatService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionRegistry.register(session);
        log.info("chat websocket connected: sessionId={}, userId={}", session.getId(), session.getAttributes().get(WebSocketConstants.ATTR_USER_ID));
        session.sendMessage(messageCodec.buildReady(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(messageCodec.buildPong(null));
            return;
        }
        if (!JsonUtils.isValidJson(payload)) {
            session.sendMessage(messageCodec.buildJsonError(null));
            return;
        }

        ChatWsRequest request;
        try {
            request = messageCodec.decode(payload);
        } catch (BusinessException ex) {
            session.sendMessage(messageCodec.buildBusinessError(null, ex.getCode(), ex.getMessage()));
            return;
        } catch (Exception ex) {
            log.warn("chat websocket request decode failed: sessionId={}, userId={}",
                    session.getId(), session.getAttributes().get(WebSocketConstants.ATTR_USER_ID), ex);
            session.sendMessage(messageCodec.buildJsonError(null));
            return;
        }
        if (request == null || !StringUtils.hasText(request.getType())) {
            session.sendMessage(messageCodec.buildIllegalArgument(null, "WebSocket 消息缺少 type"));
            return;
        }

        ChatWsMessageType messageType = ChatWsMessageType.fromValue(request.getType()).orElse(null);
        if (messageType == null) {
            session.sendMessage(messageCodec.buildUnsupported(request.getRequestId(), request.getType()));
            return;
        }

        try {
            switch (messageType) {
                case PING -> session.sendMessage(messageCodec.buildPong(request.getRequestId()));
                case SEND_MESSAGE -> handleSendMessage(session, request);
                case MARK_READ -> handleMarkRead(session, request);
                default ->
                        session.sendMessage(messageCodec.buildIllegalArgument(request.getRequestId(), "当前消息类型不允许由客户端直接发送: " + request.getType()));
            }
        } catch (BusinessException ex) {
            session.sendMessage(messageCodec.buildBusinessError(request.getRequestId(), ex.getCode(), ex.getMessage()));
        } catch (Exception ex) {
            log.warn("chat websocket request failed: sessionId={}, userId={}, type={}", session.getId(), resolveUserId(session), request.getType(), ex);
            session.sendMessage(messageCodec.buildBusinessError(request.getRequestId(), ResultErrorCode.SYSTEM_ERROR.getCode(), ResultErrorCode.SYSTEM_ERROR.getMessage()));
        }
    }

    private void handleSendMessage(WebSocketSession session, ChatWsRequest request) throws Exception {
        ChatSendTextRequest sendRequest = messageCodec.decodePayload(request, ChatSendTextRequest.class);
        if (sendRequest == null) {
            session.sendMessage(messageCodec.buildIllegalArgument(request.getRequestId(), "send_message payload 不能为空"));
            return;
        }
        var message = userChatService.sendTextMessage(resolveUserId(session), sendRequest);
        session.sendMessage(messageCodec.buildAck(request.getRequestId(), ChatWsAckPayload.builder()
                .requestType(ChatWsMessageType.SEND_MESSAGE.getValue())
                .data(message)
                .build()));
    }

    private void handleMarkRead(WebSocketSession session, ChatWsRequest request) throws Exception {
        ChatWsMarkReadPayload readPayload = messageCodec.decodePayload(request, ChatWsMarkReadPayload.class);
        if (readPayload == null || readPayload.getConversationId() == null || readPayload.getReadMessageId() == null) {
            session.sendMessage(messageCodec.buildIllegalArgument(request.getRequestId(), "mark_read payload 缺少 conversationId 或 readMessageId"));
            return;
        }
        var state = userChatService.markRead(resolveUserId(session), readPayload.getConversationId(), readPayload.getReadMessageId());
        session.sendMessage(messageCodec.buildAck(request.getRequestId(), ChatWsAckPayload.builder()
                .requestType(ChatWsMessageType.MARK_READ.getValue())
                .data(state)
                .build()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
        log.info("chat websocket closed: sessionId={}, userId={}, code={}", session.getId(), session.getAttributes().get(WebSocketConstants.ATTR_USER_ID), status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("chat websocket transport error: sessionId={}, userId={}", session.getId(), session.getAttributes().get(WebSocketConstants.ATTR_USER_ID), exception);
        sessionRegistry.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(WebSocketConstants.ATTR_USER_ID);
        if (value instanceof Long userId) {
            return userId;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.LOGIN_REQUIRED);
        return 0L;
    }
}
