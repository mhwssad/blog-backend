package com.cybzacg.blogbackend.module.chat.shared.model.data;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台消息分页查询结果。
 */
@Data
public class ChatAdminMessageItem {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String messageType;
    private String content;
    private String payloadJson;
    private Long replyMessageId;
    private String clientMessageId;
    private Integer sendStatus;
    private Integer revokeStatus;
    private Long revokedBy;
    private LocalDateTime revokedAt;
    private Long totalRecipientCount;
    private Long deliveredRecipientCount;
    private Long readRecipientCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
