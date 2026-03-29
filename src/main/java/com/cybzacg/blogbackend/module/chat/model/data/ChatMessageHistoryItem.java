package com.cybzacg.blogbackend.module.chat.model.data;

import java.util.Date;
import lombok.Data;

/**
 * 消息历史查询结果。
 */
@Data
public class ChatMessageHistoryItem {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String messageType;
    private String content;
    private String payloadJson;
    private Long replyMessageId;
    private String clientMessageId;
    private Integer deliveryStatus;
    private Integer revokeStatus;
    private Date readAt;
    private Date createdAt;
    private Date updatedAt;
}
