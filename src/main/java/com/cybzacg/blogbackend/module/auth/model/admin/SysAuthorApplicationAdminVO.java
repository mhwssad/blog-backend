package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作者申请后台视图。
 */
@Data
@Schema(description = "作者申请后台视图")
public class SysAuthorApplicationAdminVO {
    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请用户ID")
    private Long userId;

    @Schema(description = "申请用户名")
    private String username;

    @Schema(description = "申请用户昵称")
    private String nickname;

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充")
    private Integer applyStatus;

    @Schema(description = "申请状态文案")
    private String applyStatusLabel;

    @Schema(description = "申请说明")
    private String applyReason;

    @Schema(description = "擅长内容方向")
    private String contentDirection;

    @Schema(description = "个人简介")
    private String introduction;

    @Schema(description = "示例链接列表")
    private List<String> sampleLinks;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核人用户名")
    private String reviewerUsername;

    @Schema(description = "审核人昵称")
    private String reviewerNickname;

    @Schema(description = "审核备注")
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
