package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI渠道配置保存请求。
 */
@Data
@Schema(description = "AI渠道配置保存请求")
public class AiChannelConfigSaveRequest {
    @NotBlank(message = "渠道编码不能为空")
    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelCode;

    @NotBlank(message = "渠道名称不能为空")
    @Schema(description = "渠道名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelName;

    @NotBlank(message = "提供方不能为空")
    @Schema(description = "提供方", requiredMode = Schema.RequiredMode.REQUIRED)
    private String provider;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modelName;

    @Schema(description = "接口基础地址")
    private String apiBaseUrl;

    @Schema(description = "加密后的API Key")
    private String apiKeyEncrypted;

    @Schema(description = "全局每日额度，0表示不限制")
    private Integer dailyQuota;

    @Schema(description = "单用户每日额度，0表示不限制")
    private Integer userDailyQuota;

    @Schema(description = "上下文长度上限，0表示不限制")
    private Integer maxContextTokens;

    @Schema(description = "可读取数据范围配置JSON")
    private String dataScopeJson;

    @Schema(description = "系统提示词模板")
    private String systemPromptTemplate;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "是否默认渠道：0-否，1-是")
    private Integer isDefault;

    @Schema(description = "二次验证票据（修改高风险字段时必填）")
    private String mfaTicket;
}
