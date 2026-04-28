package com.cybzacg.blogbackend.module.auth.experience.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端手动调整等级/经验请求。
 */
@Data
@Schema(description = "等级/经验调整请求")
public class UserLevelAdjustRequest {

    @NotNull(message = "调整类型不能为空")
    @Schema(description = "调整类型：level 或 experience", example = "experience")
    private String adjustType;

    @NotNull(message = "调整值不能为空")
    @Schema(description = "调整值（设置等级或增减经验，经验支持负数）", example = "100")
    private Integer value;

    @Schema(description = "调整原因")
    private String reason;
}
