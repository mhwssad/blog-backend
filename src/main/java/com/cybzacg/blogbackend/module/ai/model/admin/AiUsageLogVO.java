package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI使用日志信息。
 */
@Data
@Schema(description = "AI使用日志信息")
public class AiUsageLogVO {
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "请求场景类型")
    private String requestSceneType;

    @Schema(description = "请求token数")
    private Integer requestTokens;

    @Schema(description = "响应token数")
    private Integer responseTokens;

    @Schema(description = "总token数")
    private Integer totalTokens;

    @Schema(description = "额度消耗")
    private Integer quotaCost;

    @Schema(description = "成功状态：0-失败，1-成功")
    private Integer successStatus;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
