package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息表。
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 所属会话ID
     */
    private Long conversationId;
    /**
     * 发送者ID
     */
    private Long senderId;
    /**
     * 消息类型（text-文本，image-图片，file-文件，system-系统）
     */
    private String messageType;
    /**
     * 消息文本内容
     */
    private String content;
    /**
     * 消息附加数据（JSON格式，含附件URL等）
     */
    private String payloadJson;
    /**
     * 回复目标消息ID
     */
    private Long replyMessageId;
    /**
     * 是否@全体：0-否，1-是
     */
    private Integer mentionAll;
    /**
     * 被@的用户ID列表（逗号分隔）
     */
    private String mentionedUserIds;
    /**
     * 发送状态：0-发送中，1-已发送，2-发送失败
     */
    private Integer sendStatus;
    /**
     * 撤回状态：0-正常，1-已撤回
     */
    private Integer revokeStatus;
    /**
     * 撤回操作者ID
     */
    private Long revokedBy;
    /**
     * 撤回时间
     */
    private LocalDateTime revokedAt;
    /**
     * 客户端消息唯一标识（用于幂等去重）
     */
    private String clientMessageId;
    /**
     * 置顶操作人ID（NULL 表示未置顶）
     */
    private Long pinnedBy;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
