package com.cybzacg.blogbackend.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 渠道账号池表。
 */
@Data
@TableName("ai_channel_account")
public class AiChannelAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属渠道配置ID */
    private Long channelConfigId;
    /** 账号名称（备注） */
    private String accountName;
    /** 提供方（deepseek/openai/zhipu等） */
    private String provider;
    /** 模型名称 */
    private String modelName;
    /** 接口基础地址 */
    private String apiBaseUrl;
    /** API Key */
    private String apiKeyEncrypted;
    /** 权重 */
    private Integer weight;
    /** 状态：0-停用，1-启用 */
    private Integer status;
    /** 每日额度：0-不限 */
    private Integer dailyQuota;
    /** 连续错误次数 */
    private Integer consecutiveErrors;
    /** 最大连续错误次数 */
    private Integer maxConsecutiveErrors;
    /** 最近错误时间 */
    private LocalDateTime lastErrorAt;
    /** 最近错误信息 */
    private String lastErrorMessage;
    /** 自动禁用时间 */
    private LocalDateTime disabledAt;
    /** 计划自动恢复时间 */
    private LocalDateTime autoRecoverAt;
    /** 累计调用次数 */
    private Long totalCallCount;
    /** 最近使用时间 */
    private LocalDateTime lastUsedAt;
    /** 创建人ID */
    private Long createdBy;
    /** 更新人ID */
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
