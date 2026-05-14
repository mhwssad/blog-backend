package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建AI会话请求。
 */
@Data
@Schema(description = "创建AI会话请求")
public class AiSessionCreateRequest {
    @Schema(description = "渠道配置ID，不填则使用默认渠道")
    private Long channelConfigId;

    @Size(max = 64, message = "会话标题最多64个字符")
    @Schema(description = "会话标题")
    private String title;

    @Size(max = 32, message = "会话场景最多32个字符")
    @Schema(description = "会话场景", example = "general")
    private String sceneType = "general";
}
