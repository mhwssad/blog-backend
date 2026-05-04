package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识源配置更新请求。
 */
@Data
@Schema(description = "知识源配置更新请求")
public class AiKnowledgeSourceConfigSaveRequest {
    @NotNull(message = "同步间隔不能为空")
    @Schema(description = "同步间隔（秒）")
    private Integer syncInterval;

    @Schema(description = "扩展配置JSON")
    private String configJson;

    @Schema(description = "备注")
    private String remark;
}
