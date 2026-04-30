package com.cybzacg.blogbackend.module.auth.config.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "系统配置信息")
public class SysConfigAdminVO {
    @Schema(description = "配置ID")
    private Long id;
    @Schema(description = "配置名称")
    private String configName;
    @Schema(description = "配置键")
    private String configKey;
    @Schema(description = "配置值")
    private String configValue;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
