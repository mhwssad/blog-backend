package com.cybzacg.blogbackend.module.auth.experience.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 经验流水分页查询参数。
 */
@Data
@Schema(description = "经验流水分页查询")
public class ExperienceLogPageQuery {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "经验来源类型")
    private String sourceType;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "当前页", example = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;
}
