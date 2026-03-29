package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 聊天会话已读游标表。
 */
@Data
@TableName("chat_message_read_cursor")
public class ChatMessageReadCursor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long userId;
    private Long readMessageId;
    private Date readAt;
    private Long deliveredMessageId;
    private Date deliveredAt;
    private Integer unreadCount;
    private Date createdAt;
    private Date updatedAt;
}
