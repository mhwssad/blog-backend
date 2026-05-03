package com.cybzacg.blogbackend.module.ai.model.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI模型调用结果。
 */
@Data
@Schema(description = "AI模型调用结果")
public class AiModelCallResult {
    @Schema(description = "响应内容")
    private String content;

    @Schema(description = "请求token数")
    private int requestTokens;

    @Schema(description = "响应token数")
    private int responseTokens;

    @Schema(description = "总token数")
    private int totalTokens;

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;
}
