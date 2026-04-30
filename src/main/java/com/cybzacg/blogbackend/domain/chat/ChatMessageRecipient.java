package com.cybzacg.blogbackend.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息接收状态表。
 */
@Data
@TableName("chat_message_recipient")
public class ChatMessageRecipient {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 消息ID
     */
    private Long messageId;
    /**
     * 会话ID
     */
    private Long conversationId;
    /**
     * 接收者ID
     */
    private Long recipientUserId;
    /**
     * 接收类型（single-单聊，group-群聊）
     */
    private String receiveType;
    /**
     * 投递状态：0-待投递，1-已投递，2-已读
     */
    private Integer deliveryStatus;
    /**
     * 投递时间
     */
    private LocalDateTime deliveredAt;
    /**
     * 已读时间
     */
    private LocalDateTime readAt;
    /**
     * 可见状态：0-正常，1-已隐藏
     */
    private Integer visibleStatus;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
