package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.validation.EnumValue;
import com.cybzacg.blogbackend.enums.ai.AiChannelStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI渠道状态变更请求。
 */
@Data
@Schema(description = "AI渠道状态变更请求")
public class AiChannelStatusRequest {
    @NotNull(message = "状态不能为空")
    @EnumValue(enumClass = AiChannelStatusEnum.class, message = "渠道状态值无效")
    @Schema(description = "状态：0-停用，1-启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
