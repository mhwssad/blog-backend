package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 聊天消息表。
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String messageType;
    private String content;
    private String payloadJson;
    private Long replyMessageId;
    private Integer mentionAll;
    private String mentionedUserIds;
    private Integer sendStatus;
    private Integer revokeStatus;
    private Long revokedBy;
    private Date revokedAt;
    private String clientMessageId;
    private Date createdAt;
    private Date updatedAt;
}
