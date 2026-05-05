package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 工具执行结果。
 */
@Data
@Schema(description = "AI 工具执行结果")
public class AiToolExecuteVO {
    @Schema(description = "是否成功")
    private Boolean success;
    @Schema(description = "结果文本")
    private String resultText;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "耗时毫秒")
    private Long elapsedMs;
    @Schema(description = "调用日志 ID")
    private Long callLogId;
}
