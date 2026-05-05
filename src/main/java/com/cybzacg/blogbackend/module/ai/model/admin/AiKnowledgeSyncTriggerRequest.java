package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识同步触发请求。
 */
@Data
@Schema(description = "知识同步触发请求")
public class AiKnowledgeSyncTriggerRequest {
    @NotBlank(message = "知识源类型不能为空")
    @Schema(description = "知识源类型")
    private String sourceType;

    @Schema(description = "任务类型，默认 full_sync")
    private String taskType = "full_sync";

    @Schema(description = "来源对象ID，single_entry 时必填")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;
}
