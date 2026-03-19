package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "系统配置新增/修改请求")
public class SysConfigSaveRequest {
    @NotBlank(message = "配置名称不能为空")
    @Schema(description = "配置名称")
    private String configName;

    @NotBlank(message = "配置键不能为空")
    @Schema(description = "配置键")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "备注")
    private String remark;
}
