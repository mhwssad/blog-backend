package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户频道创建申请视图。
 */
@Data
@Schema(description = "用户频道创建申请视图")
public class ChatChannelApplicationVO {
    private Long id;
    private Long applicantUserId;
    private String desiredName;
    private String desiredSceneType;
    private String desiredCategoryCode;
    private String description;
    private Integer applyStatus;
    private String applyStatusLabel;
    private Long conversationId;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
