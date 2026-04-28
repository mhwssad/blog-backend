package com.cybzacg.blogbackend.module.auth.experience.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 用户经验来源汇总视图。
 */
@Data
@Builder
@Schema(description = "用户经验来源汇总")
public class UserExperienceSummaryVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "当前等级")
    private Integer level;

    @Schema(description = "等级称号")
    private String title;

    @Schema(description = "当前经验值")
    private Integer experiencePoints;

    @Schema(description = "今日已获得经验")
    private Integer todayXp;

    @Schema(description = "登录经验总计")
    private Integer dailyLoginXp;

    @Schema(description = "发文经验总计")
    private Integer articlePublishXp;

    @Schema(description = "评论经验总计")
    private Integer commentCreateXp;

    @Schema(description = "点赞经验总计")
    private Integer likeGivenXp;

    @Schema(description = "被点赞经验总计")
    private Integer likeReceivedXp;

    @Schema(description = "聊天经验总计")
    private Integer chatMessageXp;
}
