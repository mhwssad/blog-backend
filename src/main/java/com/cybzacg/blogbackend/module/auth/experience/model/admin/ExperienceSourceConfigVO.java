package com.cybzacg.blogbackend.module.auth.experience.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 经验来源配置视图。
 */
@Data
@Builder
@Schema(description = "经验来源配置项")
public class ExperienceSourceConfigVO {

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "备注")
    private String remark;
}
