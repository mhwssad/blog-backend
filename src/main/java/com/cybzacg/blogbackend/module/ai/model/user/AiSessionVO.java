package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI会话信息。
 */
@Data
@Schema(description = "AI会话信息")
public class AiSessionVO {
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "会话场景")
    private String sceneType;

    @Schema(description = "状态：0-关闭，1-正常")
    private Integer status;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
