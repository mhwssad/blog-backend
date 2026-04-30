package com.cybzacg.blogbackend.module.auth.author.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 作者申请状态修正请求。
 */
@Data
@Schema(description = "作者申请状态修正请求")
public class SysAuthorApplicationRepairRequest {
    @NotNull(message = "目标状态不能为空")
    @Schema(description = "目标状态：0 待审核，1 已通过，2 已拒绝，3 待补充")
    private Integer targetStatus;

    @Size(max = 512, message = "修正备注长度不能超过512个字符")
    @Schema(description = "修正备注")
    private String reviewComment;
}
