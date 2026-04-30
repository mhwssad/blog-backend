package com.cybzacg.blogbackend.module.chat.websocket.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * 聊天 WebSocket 消息类型。
 */
public enum ChatWsMessageType {
    READY("ready"),
    PING("ping"),
    PONG("pong"),
    SEND_MESSAGE("send_message"),
    MARK_READ("mark_read"),
    MESSAGE_CREATED("message_created"),
    MESSAGE_UPDATED("message_updated"),
    MESSAGE_REVOKED("message_revoked"),
    MESSAGE_DELETED("message_deleted"),
    READ_UPDATED("read_updated"),
    CONVERSATION_UPDATED("conversation_updated"),
    MEMBERS_UPDATED("members_updated"),
    ACK("ack"),
    ERROR("error");

    private final String value;

    ChatWsMessageType(String value) {
        this.value = value;
    }

    public static Optional<ChatWsMessageType> fromValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equalsIgnoreCase(value))
                .findFirst();
    }

    public String getValue() {
        return value;
    }
}
