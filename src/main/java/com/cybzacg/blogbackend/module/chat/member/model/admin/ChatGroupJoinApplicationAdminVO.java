package com.cybzacg.blogbackend.module.chat.member.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台群入群申请视图。
 */
@Data
@Schema(description = "后台群入群申请视图")
public class ChatGroupJoinApplicationAdminVO {
    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "会话名称")
    private String conversationName;

    @Schema(description = "会话头像")
    private String conversationAvatar;

    @Schema(description = "会话类型")
    private String conversationType;

    @Schema(description = "业务场景")
    private String conversationSceneType;

    @Schema(description = "加入规则")
    private String conversationJoinRule;

    @Schema(description = "会话状态")
    private Integer conversationStatus;

    @Schema(description = "申请用户ID")
    private Long applicantUserId;

    @Schema(description = "申请用户用户名")
    private String applicantUsername;

    @Schema(description = "申请用户昵称")
    private String applicantNickname;

    @Schema(description = "申请用户头像")
    private String applicantAvatar;

    @Schema(description = "申请附言")
    private String applyMessage;

    @Schema(description = "申请状态")
    private Integer applyStatus;

    @Schema(description = "申请状态标签")
    private String applyStatusLabel;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人用户名")
    private String reviewerUsername;

    @Schema(description = "审核人昵称")
    private String reviewerNickname;

    @Schema(description = "审核意见")
    private String reviewComment;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
