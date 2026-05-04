package com.cybzacg.blogbackend.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一禁言记录表。
 */
@Data
@TableName("chat_user_mute_record")
public class ChatUserMuteRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 被禁言用户 ID */
    private Long userId;
    /** 禁言范围：global/lobby/topic_channel/group */
    private String scope;
    /** 关联会话 ID（lobby/topic_channel/group 时必填） */
    private Long conversationId;
    /** 禁言截止时间（NULL 表示永久禁言） */
    private LocalDateTime muteUntil;
    /** 0-已解除 1-生效中 */
    private Integer status;
    /** 禁言原因 */
    private String reason;
    /** 来源：admin/report/auto */
    private String sourceType;
    /** 关联举报 ID */
    private Long reportId;
    /** 操作人 ID */
    private Long operatorId;
    /** 解除人 ID */
    private Long releasedBy;
    /** 解除时间 */
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
