package com.cybzacg.blogbackend.module.chat.member.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台频道创建申请视图。
 */
@Data
@Schema(description = "后台频道创建申请视图")
public class ChatChannelApplicationAdminVO {
    private Long id;
    private Long applicantUserId;
    private String applicantUsername;
    private String applicantNickname;
    private String applicantAvatar;
    private String desiredName;
    private String desiredSceneType;
    private String desiredCategoryCode;
    private String description;
    private Integer applyStatus;
    private String applyStatusLabel;
    private Long conversationId;
    private Long reviewerId;
    private String reviewerUsername;
    private String reviewerNickname;
    private String reviewComment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
