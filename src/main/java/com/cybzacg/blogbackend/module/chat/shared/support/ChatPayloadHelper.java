package com.cybzacg.blogbackend.module.chat.shared.support;

import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatReplyMessageVO;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 聊天消息载荷解析与构造共享组件。
 */
@Component
public class ChatPayloadHelper {

    public ChatMessagePayloadVO parseMessagePayload(String payloadJson) {
        if (!StrUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            ChatMessagePayloadVO payload = JsonUtils.fromJson(payloadJson, ChatMessagePayloadVO.class);
            if (payload != null && (payload.getFile() != null || payload.getReply() != null)) {
                return payload;
            }
            ChatFilePayloadVO legacyFilePayload = JsonUtils.fromJson(payloadJson, ChatFilePayloadVO.class);
            if (hasFilePayloadContent(legacyFilePayload)) {
                ChatMessagePayloadVO legacyPayload = new ChatMessagePayloadVO();
                legacyPayload.setFile(legacyFilePayload);
                return legacyPayload;
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return null;
    }

    public ChatFilePayloadVO extractFilePayload(String payloadJson) {
        ChatMessagePayloadVO payload = parseMessagePayload(payloadJson);
        return payload == null ? null : payload.getFile();
    }

    public ChatReplyMessageVO extractReplyPayload(String payloadJson) {
        ChatMessagePayloadVO payload = parseMessagePayload(payloadJson);
        return normalizeReplySnapshot(payload == null ? null : payload.getReply());
    }

    public ChatReplyMessageVO normalizeReplySnapshot(ChatReplyMessageVO reply) {
        if (reply == null) {
            return null;
        }
        if (!StrUtils.hasText(reply.getState())) {
            if (Boolean.TRUE.equals(reply.getDeleted())) {
                reply.setState(ChatConstants.REPLY_STATE_UNAVAILABLE);
            } else if (Boolean.TRUE.equals(reply.getRevoked())) {
                reply.setState(ChatConstants.REPLY_STATE_REVOKED);
            } else {
                reply.setState(ChatConstants.REPLY_STATE_NORMAL);
            }
        }
        return reply;
    }

    public ChatReplyMessageVO buildUnavailableReplySnapshot(Long replyMessageId) {
        ChatReplyMessageVO reply = new ChatReplyMessageVO();
        reply.setId(replyMessageId);
        reply.setContent(ChatConstants.REPLY_MESSAGE_UNAVAILABLE_PLACEHOLDER);
        reply.setDeleted(true);
        reply.setRevoked(false);
        reply.setState(ChatConstants.REPLY_STATE_UNAVAILABLE);
        return reply;
    }

    public boolean isEdited(String messageType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_TEXT)
                && createdAt != null
                && updatedAt != null
                && updatedAt.isAfter(createdAt);
    }

    public boolean isAttachmentMessageType(String messageType) {
        return Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_FILE)
                || Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE)
                || Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE);
    }

    public boolean hasFilePayloadContent(ChatFilePayloadVO payload) {
        return payload != null
                && (payload.getBusinessId() != null
                || payload.getFileId() != null
                || StrUtils.hasText(payload.getFileName())
                || StrUtils.hasText(payload.getFileUrl()));
    }
}
