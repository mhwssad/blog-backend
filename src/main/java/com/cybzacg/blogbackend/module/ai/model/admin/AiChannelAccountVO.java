package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 渠道账号信息。
 */
@Data
@Schema(description = "AI渠道账号信息")
public class AiChannelAccountVO {
    @Schema(description = "账号ID")
    private Long id;

    @Schema(description = "所属渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "账号名称（备注）")
    private String accountName;

    @Schema(description = "提供方")
    private String provider;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "接口基础地址")
    private String apiBaseUrl;

    @Schema(description = "API Key（脱敏）")
    private String apiKeyEncrypted;

    @Schema(description = "权重")
    private Integer weight;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "每日额度：0-不限")
    private Integer dailyQuota;

    @Schema(description = "连续错误次数")
    private Integer consecutiveErrors;

    @Schema(description = "最大连续错误次数")
    private Integer maxConsecutiveErrors;

    @Schema(description = "最近错误时间")
    private LocalDateTime lastErrorAt;

    @Schema(description = "最近错误信息")
    private String lastErrorMessage;

    @Schema(description = "自动禁用时间")
    private LocalDateTime disabledAt;

    @Schema(description = "计划自动恢复时间")
    private LocalDateTime autoRecoverAt;

    @Schema(description = "累计调用次数")
    private Long totalCallCount;

    @Schema(description = "最近使用时间")
    private LocalDateTime lastUsedAt;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "更新人ID")
    private Long updatedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
