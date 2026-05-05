package com.cybzacg.blogbackend.domain.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 渠道配置表。
 */
@Data
@TableName("ai_channel_config")
public class AiChannelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 渠道编码 */
    private String channelCode;
    /** 渠道名称 */
    private String channelName;
    /** 全局每日额度：0-不限制 */
    private Integer dailyQuota;
    /** 单用户每日额度：0-不限制 */
    private Integer userDailyQuota;
    /** 上下文长度上限：0-不限制 */
    private Integer maxContextTokens;
    /** 单次输入最大 token 预算（null=不限） */
    private Integer maxInputTokens;
    /** 历史上下文最大 token 预算（null=不限） */
    private Integer maxHistoryTokens;
    /** RAG 上下文最大 token 预算（null=不限） */
    private Integer maxRagTokens;
    /** 附件最大 token 预算（null=不限） */
    private Integer maxAttachmentTokens;
    /** 输出最大 token 预算（null=不限，覆盖默认 maxTokens） */
    private Integer maxOutputTokens;
    /** 可读取数据范围配置JSON */
    private String dataScopeJson;
    /** 系统提示词模板 */
    private String systemPromptTemplate;
    /** 状态：0-停用，1-启用 */
    private Integer status;
    /** 是否默认渠道：0-否，1-是 */
    private Integer isDefault;
    /** 创建人ID */
    private Long createdBy;
    /** 更新人ID */
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
