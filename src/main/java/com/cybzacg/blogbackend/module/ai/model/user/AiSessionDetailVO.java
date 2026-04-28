package com.cybzacg.blogbackend.module.ai.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI会话详情（含渠道与模型信息）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI会话详情")
public class AiSessionDetailVO extends AiSessionVO {
    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "模型名称")
    private String modelName;
}
