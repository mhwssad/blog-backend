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
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会话ID */
    private Long conversationId;
    /** 用户ID */
    private Long userId;
    /** 已读位置消息ID */
    private Long readMessageId;
    /** 已读时间 */
    private Date readAt;
    /** 已投递位置消息ID */
    private Long deliveredMessageId;
    /** 已投递时间 */
    private Date deliveredAt;
    /** 未读消息数 */
    private Integer unreadCount;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
}
