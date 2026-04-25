package com.cybzacg.blogbackend.module.follow.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 关注关系后台视图。
 */
@Data
@Schema(description = "关注关系后台视图")
public class FollowAdminRelationVO {
    @Schema(description = "关系ID")
    private Long relationId;

    @Schema(description = "关注者用户ID")
    private Long followerId;

    @Schema(description = "关注者用户名")
    private String followerUsername;

    @Schema(description = "关注者昵称")
    private String followerNickname;

    @Schema(description = "关注者状态：0-禁用，1-启用")
    private Integer followerStatus;

    @Schema(description = "关注者是否已删除：0-否，1-是")
    private Integer followerDeletedFlag;

    @Schema(description = "被关注者用户ID")
    private Long followingId;

    @Schema(description = "被关注者用户名")
    private String followingUsername;

    @Schema(description = "被关注者昵称")
    private String followingNickname;

    @Schema(description = "被关注者状态：0-禁用，1-启用")
    private Integer followingStatus;

    @Schema(description = "被关注者是否已删除：0-否，1-是")
    private Integer followingDeletedFlag;

    @Schema(description = "关注状态：0-已取关，1-已关注")
    private Integer followStatus;

    @Schema(description = "是否特别关注：0-否，1-是")
    private Integer isSpecialFollow;

    @Schema(description = "关注来源")
    private String source;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最近关注时间")
    private LocalDateTime followTime;

    @Schema(description = "最近取关时间")
    private LocalDateTime unfollowTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
