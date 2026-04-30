package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入群申请视图。
 */
@Data
@Schema(description = "入群申请视图")
public class ChatGroupJoinApplicationVO {
    private Long id;
    private Long conversationId;
    private Long applicantUserId;
    private String applicantUsername;
    private String applicantNickname;
    private String applicantAvatar;
    private String applyMessage;
    private Integer applyStatus;
    private String applyStatusLabel;
    private Long reviewerId;
    private String reviewerUsername;
    private String reviewerNickname;
    private String reviewComment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
