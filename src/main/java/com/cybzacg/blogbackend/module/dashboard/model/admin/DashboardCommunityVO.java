package com.cybzacg.blogbackend.module.dashboard.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 后台社区统计指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Schema(description = "论坛发帖数")
    private Long forumPostCount;

    @Schema(description = "论坛回复数")
    private Long forumReplyCount;

    @Schema(description = "热门版块 Top 5")
    private List<DashboardHotSectionVO> hotSections;
}
