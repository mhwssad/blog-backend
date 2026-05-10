package com.cybzacg.blogbackend.dto.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊入群申请表。
 */
@Data
@TableName("chat_group_join_application")
public class ChatGroupJoinApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会话/群ID */
    private Long conversationId;
    /** 申请用户ID */
    private Long applicantUserId;
    /** 申请附言 */
    private String applyMessage;
    /** 申请状态：0-待审核，1-已通过，2-已拒绝，3-已取消 */
    private Integer applyStatus;
    /** 审核人ID */
    private Long reviewerId;
    /** 审核意见 */
    private String reviewComment;
    /** 提交时间 */
    private LocalDateTime submittedAt;
    /** 审核时间 */
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
