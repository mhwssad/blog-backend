package com.cybzacg.blogbackend.module.ai.model.admin;

import com.cybzacg.blogbackend.core.validation.ValidJsonObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识源配置更新请求。
 */
@Data
@Schema(description = "知识源配置更新请求")
public class AiKnowledgeSourceConfigSaveRequest {
    @NotNull(message = "同步间隔不能为空")
    @Min(value = 1, message = "同步间隔必须大于 0")
    @Schema(description = "同步间隔（秒）")
    private Integer syncInterval;

    @ValidJsonObject(message = "扩展配置必须是 JSON 对象")
    @Schema(description = "扩展配置JSON")
    private String configJson;

    @Schema(description = "备注")
    private String remark;
}
