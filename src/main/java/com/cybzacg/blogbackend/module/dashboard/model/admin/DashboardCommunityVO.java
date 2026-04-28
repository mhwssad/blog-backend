package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 后台社区统计指标。
 */
@Data
@Builder
@Schema(description = "后台社区统计指标")
public class DashboardCommunityVO {
    private DashboardRangeVO range;
    private Long chatMessageCount;
    private Long lobbyMessageCount;
    private Long groupCount;
}
