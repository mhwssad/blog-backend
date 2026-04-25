package com.cybzacg.blogbackend.module.chat.model.data;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话列表查询结果。
 */
@Data
public class ChatConversationListItem {
    private Long id;
    private String conversationType;
    private String name;
    private String avatar;
    private Long ownerId;
    private String notice;
    private Integer isAllSite;
    private Integer status;
    private String selfRole;
    private Long lastReadMessageId;
    private LocalDateTime lastReadAt;
    private Long lastDeliveredMessageId;
    private LocalDateTime lastDeliveredAt;
    private Integer unreadCount;
    private Long lastMessageId;
    private String lastMessageType;
    private String lastMessageContent;
    private Long lastMessageSenderId;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
