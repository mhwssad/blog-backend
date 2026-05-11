package com.cybzacg.blogbackend.module.auth.experience.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 经验流水分页查询参数�?
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "经验流水分页查询")
public class ExperienceLogPageQuery extends PageQuery {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "经验来源类型")
    private String sourceType;

    @Schema(description = "开始日")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;
}
