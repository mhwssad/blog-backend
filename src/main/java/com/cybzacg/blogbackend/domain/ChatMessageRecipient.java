package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 聊天消息接收状态表。
 */
@Data
@TableName("chat_message_recipient")
public class ChatMessageRecipient {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long conversationId;
    private Long recipientUserId;
    private String receiveType;
    private Integer deliveryStatus;
    private Date deliveredAt;
    private Date readAt;
    private Integer visibleStatus;
    private Date createdAt;
    private Date updatedAt;
}
