package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.validation.ValidDataScopeArray;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI渠道配置保存请求。
 */
@Data
@Schema(description = "AI渠道配置保存请求")
public class AiChannelConfigSaveRequest {
    @NotBlank(message = "渠道编码不能为空")
    @Size(max = 64, message = "渠道编码最多64个字符")
    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelCode;

    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 128, message = "渠道名称最多128个字符")
    @Schema(description = "渠道名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelName;

    @Min(value = 0, message = "全局每日额度不能为负数")
    @Schema(description = "全局每日额度，0表示不限制")
    private Integer dailyQuota;

    @Min(value = 0, message = "单用户每日额度不能为负数")
    @Schema(description = "单用户每日额度，0表示不限制")
    private Integer userDailyQuota;

    @Min(value = 0, message = "上下文长度上限不能为负数")
    @Schema(description = "上下文长度上限，0表示不限制")
    private Integer maxContextTokens;

    @Min(value = 0, message = "单次输入最大token不能为负数")
    @Schema(description = "单次输入最大token预算，null表示不限")
    private Integer maxInputTokens;

    @Min(value = 0, message = "历史上下文最大token不能为负数")
    @Schema(description = "历史上下文最大token预算，null表示不限")
    private Integer maxHistoryTokens;

    @Min(value = 0, message = "RAG上下文最大token不能为负数")
    @Schema(description = "RAG上下文最大token预算，null表示不限")
    private Integer maxRagTokens;

    @Min(value = 0, message = "附件最大token不能为负数")
    @Schema(description = "附件最大token预算，null表示不限")
    private Integer maxAttachmentTokens;

    @Min(value = 0, message = "输出最大token不能为负数")
    @Schema(description = "输出最大token预算，null表示不限")
    private Integer maxOutputTokens;

    @ValidDataScopeArray(message = "数据范围配置包含无效值")
    @Schema(description = "可读取数据范围配置JSON")
    private String dataScopeJson;

    @Schema(description = "系统提示词模板")
    private String systemPromptTemplate;

    @Min(value = 0, message = "状态值必须为 0 或 1")
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Min(value = 0, message = "默认标记必须为 0 或 1")
    @Schema(description = "是否默认渠道：0-否，1-是")
    private Integer isDefault;

    @Schema(description = "二次验证票据（修改高风险字段时必填）")
    private String mfaTicket;
}
