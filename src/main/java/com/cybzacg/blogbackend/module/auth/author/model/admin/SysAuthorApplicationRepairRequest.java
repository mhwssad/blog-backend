package com.cybzacg.blogbackend.module.auth.author.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.enums.auth.AuthorApplicationStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    @EnumValue(enumClass = AuthorApplicationStatusEnum.class, message = "目标状态不合法")
    @Schema(description = "目标状态：0 待审核，1 已通过，2 已拒绝，3 待补充")
    private Integer targetStatus;

    @NotBlank(message = "修正作者申请状态必须填写备注")
    @Size(max = 512, message = "修正备注长度不能超过512个字符")
    @Schema(description = "修正备注")
    private String reviewComment;
}
