package com.cybzacg.blogbackend.dto.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话成员表。
 */
@Data
@TableName("chat_conversation_member")
public class ChatConversationMember {
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
     * 成员角色（owner-群主，admin-管理员，member-普通成员）
     */
    private String memberRole;
    /**
     * 加入来源（invite-邀请，link-链接，search-搜索）
     */
    private String joinSource;
    /**
     * 成员状态：0-正常，1-已退出，2-已移除
     */
    private Integer status;
    /**
     * 禁言截止时间
     */
    private LocalDateTime muteUntil;
    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;
    /**
     * 最后已读消息ID
     */
    private Long lastReadMessageId;
    /**
     * 最后已读时间
     */
    private LocalDateTime lastReadAt;
    /**
     * 最后已投递消息ID
     */
    private Long lastDeliveredMessageId;
    /**
     * 最后已投递时间
     */
    private LocalDateTime lastDeliveredAt;
    /**
     * 成员备注名
     */
    private String remark;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
