package com.cybzacg.blogbackend.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话已读游标表。
 */
@Data
@TableName("chat_message_read_cursor")
public class ChatMessageReadCursor {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 会话ID
     */
    private Long conversationId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 已读位置消息ID
     */
    private Long readMessageId;
    /**
     * 已读时间
     */
    private LocalDateTime readAt;
    /**
     * 已投递位置消息ID
     */
    private Long deliveredMessageId;
    /**
     * 已投递时间
     */
    private LocalDateTime deliveredAt;
    /**
     * 未读消息数
     */
    private Integer unreadCount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
