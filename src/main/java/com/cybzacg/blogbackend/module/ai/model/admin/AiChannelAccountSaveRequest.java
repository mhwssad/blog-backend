package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 渠道账号保存请求。
 */
@Data
@Schema(description = "AI渠道账号保存请求")
public class AiChannelAccountSaveRequest {
    @NotBlank(message = "账号名称不能为空")
    @Size(max = 128, message = "账号名称最多128个字符")
    @Schema(description = "账号名称（备注）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountName;

    @NotBlank(message = "提供方不能为空")
    @Size(max = 64, message = "提供方最多64个字符")
    @Schema(description = "提供方（deepseek/openai/zhipu等）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String provider;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "模型名称最多128个字符")
    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modelName;

    @NotBlank(message = "接口基础地址不能为空")
    @Size(max = 512, message = "接口基础地址最多512个字符")
    @Schema(description = "接口基础地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiBaseUrl;

    @NotBlank(message = "API Key不能为空")
    @Size(max = 512, message = "API Key最多512个字符")
    @Schema(description = "API Key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiKeyEncrypted;

    @Min(value = 1, message = "权重必须大于 0")
    @Schema(description = "权重，默认1")
    private Integer weight;

    @Min(value = 0, message = "状态值必须为 0 或 1")
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Min(value = 0, message = "每日额度不能为负数")
    @Schema(description = "每日额度：0-不限")
    private Integer dailyQuota;

    @Min(value = 1, message = "最大连续错误次数必须大于 0")
    @Schema(description = "最大连续错误次数，默认5")
    private Integer maxConsecutiveErrors;

    @Schema(description = "二次验证票据（修改 API Key 时必填）")
    private String mfaTicket;
}
