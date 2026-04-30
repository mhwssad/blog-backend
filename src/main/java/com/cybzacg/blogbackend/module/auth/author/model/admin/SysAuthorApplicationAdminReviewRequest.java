package com.cybzacg.blogbackend.module.auth.author.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 作者申请审核请求。
 */
@Data
@Schema(description = "作者申请审核请求")
public class SysAuthorApplicationAdminReviewRequest {
    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：1-通过，2-拒绝，3-待补充")
    private Integer reviewStatus;

    @Size(max = 512, message = "审核备注长度不能超过512个字符")
    @Schema(description = "审核备注")
    private String reviewComment;
}
