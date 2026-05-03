package com.cybzacg.blogbackend.module.auth.experience.model.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 每日经验来源聚合数据项。
 */
@Data
@Schema(description = "每日经验来源聚合数据项")
public class ExperienceDailySummaryItem {
    @Schema(description = "经验来源类型")
    private String sourceType;

    @Schema(description = "经验值总计")
    private Integer totalXp;
}
