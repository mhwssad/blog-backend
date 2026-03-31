package com.cybzacg.blogbackend.module.follow.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 互关状态视图。
 */
@Data
@Builder
@Schema(description = "互关状态")
public class UserFollowMutualVO {
    @Schema(description = "目标用户ID")
    private Long targetUserId;

    @Schema(description = "当前用户是否已关注目标用户")
    private Boolean following;

    @Schema(description = "目标用户是否已关注当前用户")
    private Boolean followedBy;

    @Schema(description = "是否互相关注")
    private Boolean mutualFollow;
}
