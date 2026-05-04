package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台看板热门论坛版块指标。
 */
@Data
@Schema(description = "后台看板热门论坛版块指标")
public class DashboardHotSectionVO {
    @Schema(description = "版块ID")
    private Long sectionId;

    @Schema(description = "版块名称")
    private String sectionName;

    @Schema(description = "发帖数")
    private Long postCount;

    @Schema(description = "回复数")
    private Long replyCount;

    @Schema(description = "热度值，按发帖数+回复数计算")
    private Long hotValue;
}
