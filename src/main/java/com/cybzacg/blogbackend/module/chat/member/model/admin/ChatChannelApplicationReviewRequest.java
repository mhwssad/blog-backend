package com.cybzacg.blogbackend.module.chat.member.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台频道创建申请审核请求。
 */
@Data
@Schema(description = "后台频道创建申请审核请求")
public class ChatChannelApplicationReviewRequest {
    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态：1-通过，2-拒绝，3-待补充", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reviewStatus;

    @Size(max = 512, message = "审核意见长度不能超过512")
    @Schema(description = "审核意见")
    private String reviewComment;
}
