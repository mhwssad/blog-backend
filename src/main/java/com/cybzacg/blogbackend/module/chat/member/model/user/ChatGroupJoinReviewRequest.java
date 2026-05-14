package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 入群申请审核请求。
 */
@Data
@Schema(description = "入群申请审核请求")
public class ChatGroupJoinReviewRequest {
    @NotNull(message = "审核状态不能为空")
    @Min(value = 1, message = "审核状态必须为 1 或 2")
    @Max(value = 2, message = "审核状态必须为 1 或 2")
    @Schema(description = "审核状态：1-通过，2-拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reviewStatus;

    @Size(max = 512, message = "审核意见长度不能超过512")
    @Schema(description = "审核意见")
    private String reviewComment;
}
