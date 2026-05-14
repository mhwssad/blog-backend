package com.cybzacg.blogbackend.module.auth.experience.model.admin;

import com.cybzacg.blogbackend.core.validation.ConditionalRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理端手动调整等级/经验请求。
 */
@Data
@ConditionalRange(field = "value", conditionField = "adjustType", conditionValue = "level", min = 1, max = 10, message = "等级必须在 1-10 之间")
@Schema(description = "等级/经验调整请求")
public class UserLevelAdjustRequest {

    @NotNull(message = "调整类型不能为空")
    @Pattern(regexp = "level|experience", message = "调整类型必须是 level 或 experience")
    @Schema(description = "调整类型：level 或 experience", example = "experience")
    private String adjustType;

    @NotNull(message = "调整值不能为空")
    @Schema(description = "调整值（设置等级或增减经验，经验支持负数）", example = "100")
    private Integer value;

    @Schema(description = "调整原因")
    private String reason;
}
