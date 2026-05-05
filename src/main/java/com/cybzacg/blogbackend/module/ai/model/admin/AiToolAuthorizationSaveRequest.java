package com.cybzacg.blogbackend.module.ai.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 工具授权创建/更新请求。
 */
@Data
@Schema(description = "AI 工具授权创建/更新请求")
public class AiToolAuthorizationSaveRequest {
    @NotNull(message = "工具 ID 不能为空")
    @Schema(description = "工具 ID")
    private Long toolId;

    @NotBlank(message = "授权类型不能为空")
    @Schema(description = "授权类型 agent/scene/permission/data_scope")
    private String authorizationType;

    @NotBlank(message = "授权键不能为空")
    @Size(max = 128, message = "授权键最多128个字符")
    @Schema(description = "授权键")
    private String authorizationKey;

    @Schema(description = "数据范围")
    private String dataScope;

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "启用状态：0-停用/1-启用")
    private Integer enabled;
}
