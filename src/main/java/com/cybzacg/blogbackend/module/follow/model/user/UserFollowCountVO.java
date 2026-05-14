package com.cybzacg.blogbackend.module.follow.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注统计视图。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "关注统计")
public class UserFollowCountVO {
    @Schema(description = "关注数")
    private Long followingCount;

    @Schema(description = "粉丝数")
    private Long fanCount;
}
