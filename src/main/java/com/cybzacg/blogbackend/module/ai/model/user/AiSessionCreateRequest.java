package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建AI会话请求。
 */
@Data
@Schema(description = "创建AI会话请求")
public class AiSessionCreateRequest {
    @Schema(description = "渠道配置ID，不填则使用默认渠道")
    private Long channelConfigId;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "会话场景", example = "general")
    private String sceneType = "general";
}
