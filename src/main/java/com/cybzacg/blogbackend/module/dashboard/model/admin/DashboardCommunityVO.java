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
    @Schema(description = "时间范围")
    private DashboardRangeVO range;

    @Schema(description = "私信消息数")
    private Long chatMessageCount;

    @Schema(description = "大厅消息数")
    private Long lobbyMessageCount;

    @Schema(description = "群组数量")
    private Long groupCount;
}
