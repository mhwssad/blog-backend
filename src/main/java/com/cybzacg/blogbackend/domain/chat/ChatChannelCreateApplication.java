package com.cybzacg.blogbackend.domain.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 频道创建申请表。
 */
@Data
@TableName("chat_channel_create_application")
public class ChatChannelCreateApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 申请用户ID */
    private Long applicantUserId;
    /** 期望频道名称 */
    private String desiredName;
    /** 期望频道类型 */
    private String desiredSceneType;
    /** 期望分类编码 */
    private String desiredCategoryCode;
    /** 申请说明 */
    private String description;
    /** 申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充 */
    private Integer applyStatus;
    /** 审核通过后关联频道ID */
    private Long conversationId;
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
