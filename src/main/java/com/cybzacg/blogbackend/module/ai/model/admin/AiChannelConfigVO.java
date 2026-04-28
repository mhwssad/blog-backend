package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI渠道配置信息。
 */
@Data
@Schema(description = "AI渠道配置信息")
public class AiChannelConfigVO {
    @Schema(description = "渠道配置ID")
    private Long id;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "提供方")
    private String provider;

    @Schema(description = "模型名称")
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

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "更新人ID")
    private Long updatedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
