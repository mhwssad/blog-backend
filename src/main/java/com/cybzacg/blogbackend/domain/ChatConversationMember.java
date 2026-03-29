package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 聊天会话成员表。
 */
@Data
@TableName("chat_conversation_member")
public class ChatConversationMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long userId;
    private String memberRole;
    private String joinSource;
    private Integer status;
    private Date muteUntil;
    private Date joinedAt;
    private Long lastReadMessageId;
    private Date lastReadAt;
    private Long lastDeliveredMessageId;
    private Date lastDeliveredAt;
    private String remark;
    private Date createdAt;
    private Date updatedAt;
}
