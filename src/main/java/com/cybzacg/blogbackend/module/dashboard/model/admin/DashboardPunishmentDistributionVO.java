package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后台看板举报处罚类型分布指标。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "后台看板举报处罚类型分布指标")
public class DashboardPunishmentDistributionVO {
    @Schema(description = "处罚类型，空值归为 none")
    private String punishmentType;

    @Schema(description = "数量")
    private Long count;
}
