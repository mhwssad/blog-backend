package com.cybzacg.blogbackend.module.chat.model.data;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台会话分页查询结果。
 */
@Data
public class ChatAdminConversationListItem {
    private Long id;
    private String conversationType;
    private String name;
    private String avatar;
    private Long ownerId;
    private String notice;
    private Integer isAllSite;
    private Integer status;
    private Long memberCount;
    private Long lastMessageId;
    private String lastMessageType;
    private String lastMessageContent;
    private Long lastMessageSenderId;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
