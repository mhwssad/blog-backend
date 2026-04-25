package com.cybzacg.blogbackend.module.follow.model.data;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 关注关系后台联表查询结果。
 */
@Data
public class FollowAdminRelationItem {
    private Long relationId;
    private Long followerId;
    private String followerUsername;
    private String followerNickname;
    private Integer followerStatus;
    private Integer followerDeletedFlag;
    private Long followingId;
    private String followingUsername;
    private String followingNickname;
    private Integer followingStatus;
    private Integer followingDeletedFlag;
    private Integer followStatus;
    private Integer isSpecialFollow;
    private String source;
    private String remark;
    private LocalDateTime followTime;
    private LocalDateTime unfollowTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
