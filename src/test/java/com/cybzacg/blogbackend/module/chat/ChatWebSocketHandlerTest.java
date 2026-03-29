package com.cybzacg.blogbackend.module.chat;

import com.cybzacg.blogbackend.common.constant.WebSocketConstants;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatSendTextRequest;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsAckPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMarkReadPayload;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsMessageType;
import com.cybzacg.blogbackend.module.chat.model.websocket.ChatWsRequest;
import com.cybzacg.blogbackend.module.chat.service.ChatWebSocketSessionRegistry;
import com.cybzacg.blogbackend.module.chat.service.UserChatService;
import com.cybzacg.blogbackend.module.chat.websocket.ChatWebSocketHandler;
import com.cybzacg.blogbackend.module.chat.websocket.ChatWebSocketMessageCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {
    @Mock
    private ChatWebSocketSessionRegistry sessionRegistry;
    @Mock
    private ChatWebSocketMessageCodec messageCodec;
    @Mock
    private UserChatService userChatService;
    @Mock
    private WebSocketSession session;

    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChatWebSocketHandler(sessionRegistry, messageCodec, userChatService);
    }

    @Test
    void afterConnectionEstablishedShouldRegisterAndSendReadyMessage() throws Exception {
        mockSessionIdAndUser();
        TextMessage readyMessage = new TextMessage("{\"type\":\"ready\"}");
        when(messageCodec.buildReady(session)).thenReturn(readyMessage);

        handler.afterConnectionEstablished(session);

        verify(sessionRegistry).register(session);
        verify(session).sendMessage(readyMessage);
    }

    @Test
    void invalidJsonShouldReturnJsonError() throws Exception {
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");
        when(messageCodec.buildJsonError(null)).thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("not-json"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void decodeShouldReturnJsonErrorWhenCodecThrowsUnexpectedException() throws Exception {
        mockSessionIdAndUser();
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenThrow(new RuntimeException("bad request"));
        when(messageCodec.buildJsonError(null)).thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void decodeShouldReturnBusinessErrorWhenCodecThrowsBusinessException() throws Exception {
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}"))
                .thenThrow(new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "协议体结构不合法"));
        when(messageCodec.buildBusinessError(null, ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "协议体结构不合法"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void rawPingShouldReturnPong() throws Exception {
        TextMessage pongMessage = new TextMessage("{\"type\":\"pong\"}");
        when(messageCodec.buildPong(null)).thenReturn(pongMessage);

        handler.handleMessage(session, new TextMessage("ping"));

        verify(session).sendMessage(pongMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void missingTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument(null, "WebSocket 消息缺少 type")).thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).markRead(eq(1L), any(Long.class), any(Long.class));
    }

    @Test
    void unsupportedTypeShouldReturnUnsupportedError() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType("revoke_message");
        request.setRequestId("req-1");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"revoke_message\"}")).thenReturn(request);
        when(messageCodec.buildUnsupported("req-1", "revoke_message")).thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"revoke_message\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void sendMessageShouldAckWithServiceResult() throws Exception {
        mockSessionUser();
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.SEND_MESSAGE.getValue());
        request.setRequestId("req-2");

        ChatSendTextRequest sendRequest = new ChatSendTextRequest();
        sendRequest.setConversationId(1001L);
        sendRequest.setContent("hello");

        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(9001L);
        TextMessage ackMessage = new TextMessage("{\"type\":\"ack\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatSendTextRequest.class)).thenReturn(sendRequest);
        when(userChatService.sendTextMessage(1L, sendRequest)).thenReturn(messageVO);
        when(messageCodec.buildAck(eq("req-2"), any(ChatWsAckPayload.class))).thenReturn(ackMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(userChatService).sendTextMessage(1L, sendRequest);
        verify(messageCodec).buildAck(eq("req-2"), argThat((ChatWsAckPayload ackPayload) ->
                ackPayload != null
                        && ChatWsMessageType.SEND_MESSAGE.getValue().equals(ackPayload.getRequestType())
                        && ackPayload.getData() == messageVO
        ));
        verify(session).sendMessage(ackMessage);
    }

    @Test
    void sendMessageShouldRejectNullPayload() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.SEND_MESSAGE.getValue());
        request.setRequestId("req-null");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatSendTextRequest.class)).thenReturn(null);
        when(messageCodec.buildIllegalArgument("req-null", "send_message payload 不能为空")).thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void typedPingShouldReturnPongWithRequestId() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.PING.getValue());
        request.setRequestId("req-ping");
        TextMessage pongMessage = new TextMessage("{\"type\":\"pong\"}");

        when(messageCodec.decode("{\"type\":\"ping\"}")).thenReturn(request);
        when(messageCodec.buildPong("req-ping")).thenReturn(pongMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        verify(session).sendMessage(pongMessage);
        verify(userChatService, never()).markRead(eq(1L), any(Long.class), any(Long.class));
    }

    @Test
    void markReadShouldRejectMissingPayloadFields() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MARK_READ.getValue());
        request.setRequestId("req-3");

        ChatWsMarkReadPayload payload = new ChatWsMarkReadPayload();
        payload.setConversationId(1001L);
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"mark_read\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatWsMarkReadPayload.class)).thenReturn(payload);
        when(messageCodec.buildIllegalArgument("req-3", "mark_read payload 缺少 conversationId 或 readMessageId"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"mark_read\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).markRead(eq(1L), any(Long.class), any(Long.class));
    }

    @Test
    void markReadShouldAckWithReadState() throws Exception {
        mockSessionUser();
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MARK_READ.getValue());
        request.setRequestId("req-4");

        ChatWsMarkReadPayload payload = new ChatWsMarkReadPayload();
        payload.setConversationId(1001L);
        payload.setReadMessageId(9001L);

        ChatReadStateVO stateVO = new ChatReadStateVO();
        stateVO.setConversationId(1001L);
        stateVO.setReadMessageId(9001L);
        TextMessage ackMessage = new TextMessage("{\"type\":\"ack\"}");

        when(messageCodec.decode("{\"type\":\"mark_read\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatWsMarkReadPayload.class)).thenReturn(payload);
        when(userChatService.markRead(1L, 1001L, 9001L)).thenReturn(stateVO);
        when(messageCodec.buildAck(eq("req-4"), any(ChatWsAckPayload.class))).thenReturn(ackMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"mark_read\"}"));

        verify(userChatService).markRead(1L, 1001L, 9001L);
        verify(messageCodec).buildAck(eq("req-4"), argThat((ChatWsAckPayload ackPayload) ->
                ackPayload != null
                        && ChatWsMessageType.MARK_READ.getValue().equals(ackPayload.getRequestType())
                        && ackPayload.getData() == stateVO
        ));
        verify(session).sendMessage(ackMessage);
    }

    @Test
    void markReadShouldReturnBusinessErrorWhenServiceThrowsBusinessException() throws Exception {
        mockSessionUser();
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MARK_READ.getValue());
        request.setRequestId("req-read-biz");

        ChatWsMarkReadPayload payload = new ChatWsMarkReadPayload();
        payload.setConversationId(1001L);
        payload.setReadMessageId(9001L);
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"mark_read\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatWsMarkReadPayload.class)).thenReturn(payload);
        when(userChatService.markRead(1L, 1001L, 9001L))
                .thenThrow(new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "消息不存在或不可访问"));
        when(messageCodec.buildBusinessError("req-read-biz", ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "消息不存在或不可访问"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"mark_read\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void markReadShouldReturnSystemErrorWhenServiceThrowsUnexpectedException() throws Exception {
        mockSessionUser();
        mockSessionIdAndUser();
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MARK_READ.getValue());
        request.setRequestId("req-read-system");

        ChatWsMarkReadPayload payload = new ChatWsMarkReadPayload();
        payload.setConversationId(1001L);
        payload.setReadMessageId(9001L);
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"mark_read\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatWsMarkReadPayload.class)).thenReturn(payload);
        when(userChatService.markRead(1L, 1001L, 9001L)).thenThrow(new RuntimeException("db down"));
        when(messageCodec.buildBusinessError("req-read-system", ResultErrorCode.SYSTEM_ERROR.getCode(), ResultErrorCode.SYSTEM_ERROR.getMessage()))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"mark_read\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void markReadShouldReturnLoginRequiredWhenSessionHasNoUser() throws Exception {
        when(session.getAttributes()).thenReturn(Map.of());
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MARK_READ.getValue());
        request.setRequestId("req-read-login");

        ChatWsMarkReadPayload payload = new ChatWsMarkReadPayload();
        payload.setConversationId(1001L);
        payload.setReadMessageId(9001L);
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"mark_read\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatWsMarkReadPayload.class)).thenReturn(payload);
        when(messageCodec.buildBusinessError("req-read-login", ResultErrorCode.LOGIN_REQUIRED.getCode(), ResultErrorCode.LOGIN_REQUIRED.getMessage()))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"mark_read\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).markRead(eq(1L), any(Long.class), any(Long.class));
    }

    @Test
    void sendMessageShouldReturnBusinessErrorWhenServiceThrowsBusinessException() throws Exception {
        mockSessionUser();
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.SEND_MESSAGE.getValue());
        request.setRequestId("req-biz");

        ChatSendTextRequest sendRequest = new ChatSendTextRequest();
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatSendTextRequest.class)).thenReturn(sendRequest);
        when(userChatService.sendTextMessage(1L, sendRequest))
                .thenThrow(new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "消息内容不能为空"));
        when(messageCodec.buildBusinessError("req-biz", ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "消息内容不能为空"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void sendMessageShouldReturnSystemErrorWhenServiceThrowsUnexpectedException() throws Exception {
        mockSessionUser();
        mockSessionIdAndUser();
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.SEND_MESSAGE.getValue());
        request.setRequestId("req-system");

        ChatSendTextRequest sendRequest = new ChatSendTextRequest();
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatSendTextRequest.class)).thenReturn(sendRequest);
        when(userChatService.sendTextMessage(1L, sendRequest)).thenThrow(new RuntimeException("db down"));
        when(messageCodec.buildBusinessError("req-system", ResultErrorCode.SYSTEM_ERROR.getCode(), ResultErrorCode.SYSTEM_ERROR.getMessage()))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void sendMessageShouldReturnBusinessErrorWhenDecodePayloadThrowsBusinessException() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.SEND_MESSAGE.getValue());
        request.setRequestId("req-decode-biz");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatSendTextRequest.class))
                .thenThrow(new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "payload 格式错误"));
        when(messageCodec.buildBusinessError("req-decode-biz", ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "payload 格式错误"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void sendMessageShouldReturnLoginRequiredWhenSessionHasNoUser() throws Exception {
        when(session.getAttributes()).thenReturn(Map.of());
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.SEND_MESSAGE.getValue());
        request.setRequestId("req-login");

        ChatSendTextRequest sendRequest = new ChatSendTextRequest();
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatSendTextRequest.class)).thenReturn(sendRequest);
        when(messageCodec.buildBusinessError("req-login", ResultErrorCode.LOGIN_REQUIRED.getCode(), ResultErrorCode.LOGIN_REQUIRED.getMessage()))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void sendMessageShouldReturnLoginRequiredWhenSessionUserIsForgedString() throws Exception {
        when(session.getAttributes()).thenReturn(Map.of(WebSocketConstants.ATTR_USER_ID, "1"));
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.SEND_MESSAGE.getValue());
        request.setRequestId("req-login-forged");

        ChatSendTextRequest sendRequest = new ChatSendTextRequest();
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"send_message\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatSendTextRequest.class)).thenReturn(sendRequest);
        when(messageCodec.buildBusinessError("req-login-forged", ResultErrorCode.LOGIN_REQUIRED.getCode(), ResultErrorCode.LOGIN_REQUIRED.getMessage()))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"send_message\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
    }

    @Test
    void serverEventTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MESSAGE_CREATED.getValue());
        request.setRequestId("req-event");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"message_created\"}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument("req-event", "当前消息类型不允许由客户端直接发送: message_created"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"message_created\"}"));

        verify(session).sendMessage(errorMessage);
        verify(messageCodec, never()).decodePayload(any(ChatWsRequest.class), any(Class.class));
        verify(userChatService, never()).sendTextMessage(eq(1L), any(ChatSendTextRequest.class));
        verify(userChatService, never()).markRead(eq(1L), any(Long.class), any(Long.class));
    }

    @Test
    void markReadShouldReturnSystemErrorWhenDecodePayloadThrowsUnexpectedException() throws Exception {
        mockSessionUser();
        mockSessionIdAndUser();
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MARK_READ.getValue());
        request.setRequestId("req-read-decode-system");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"mark_read\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatWsMarkReadPayload.class)).thenThrow(new RuntimeException("bad payload"));
        when(messageCodec.buildBusinessError("req-read-decode-system", ResultErrorCode.SYSTEM_ERROR.getCode(), ResultErrorCode.SYSTEM_ERROR.getMessage()))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"mark_read\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).markRead(eq(1L), any(Long.class), any(Long.class));
    }

    @Test
    void markReadShouldReturnBusinessErrorWhenDecodePayloadThrowsBusinessException() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MARK_READ.getValue());
        request.setRequestId("req-read-decode-biz");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"mark_read\"}")).thenReturn(request);
        when(messageCodec.decodePayload(request, ChatWsMarkReadPayload.class))
                .thenThrow(new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "payload 类型错误"));
        when(messageCodec.buildBusinessError("req-read-decode-biz", ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "payload 类型错误"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"mark_read\"}"));

        verify(session).sendMessage(errorMessage);
        verify(userChatService, never()).markRead(eq(1L), any(Long.class), any(Long.class));
    }

    @Test
    void readyTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.READY.getValue());
        request.setRequestId("req-ready");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"ready\"}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument("req-ready", "当前消息类型不允许由客户端直接发送: ready"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"ready\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void ackTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.ACK.getValue());
        request.setRequestId("req-ack");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"ack\"}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument("req-ack", "当前消息类型不允许由客户端直接发送: ack"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"ack\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void errorTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.ERROR.getValue());
        request.setRequestId("req-error");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"error\"}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument("req-error", "当前消息类型不允许由客户端直接发送: error"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"error\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void readUpdatedTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.READ_UPDATED.getValue());
        request.setRequestId("req-read-updated");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"read_updated\"}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument("req-read-updated", "当前消息类型不允许由客户端直接发送: read_updated"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"read_updated\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void messageUpdatedTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MESSAGE_UPDATED.getValue());
        request.setRequestId("req-message-updated");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"message_updated\"}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument("req-message-updated", "当前消息类型不允许由客户端直接发送: message_updated"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"message_updated\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void membersUpdatedTypeShouldReturnIllegalArgument() throws Exception {
        ChatWsRequest request = new ChatWsRequest();
        request.setType(ChatWsMessageType.MEMBERS_UPDATED.getValue());
        request.setRequestId("req-members-updated");
        TextMessage errorMessage = new TextMessage("{\"type\":\"error\"}");

        when(messageCodec.decode("{\"type\":\"members_updated\"}")).thenReturn(request);
        when(messageCodec.buildIllegalArgument("req-members-updated", "当前消息类型不允许由客户端直接发送: members_updated"))
                .thenReturn(errorMessage);

        handler.handleMessage(session, new TextMessage("{\"type\":\"members_updated\"}"));

        verify(session).sendMessage(errorMessage);
    }

    @Test
    void afterConnectionClosedShouldUnregisterSession() {
        mockSessionIdAndUser();
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionRegistry).unregister(session);
    }

    @Test
    void handleTransportErrorShouldUnregisterAndCloseOpenSession() throws Exception {
        mockSessionIdAndUser();
        when(session.isOpen()).thenReturn(true);

        handler.handleTransportError(session, new RuntimeException("broken pipe"));

        verify(sessionRegistry).unregister(session);
        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void handleTransportErrorShouldOnlyUnregisterWhenSessionAlreadyClosed() throws Exception {
        mockSessionIdAndUser();
        when(session.isOpen()).thenReturn(false);

        handler.handleTransportError(session, new RuntimeException("broken pipe"));

        verify(sessionRegistry).unregister(session);
        verify(session, never()).close(CloseStatus.SERVER_ERROR);
    }

    private void mockSessionIdAndUser() {
        when(session.getId()).thenReturn("session-1");
        when(session.getAttributes()).thenReturn(Map.of(WebSocketConstants.ATTR_USER_ID, 1L));
    }

    private void mockSessionUser() {
        when(session.getAttributes()).thenReturn(Map.of(WebSocketConstants.ATTR_USER_ID, 1L));
    }
}
