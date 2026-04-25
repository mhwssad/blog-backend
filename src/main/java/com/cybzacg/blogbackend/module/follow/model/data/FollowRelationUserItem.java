package com.cybzacg.blogbackend.module.follow.model.data;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系列表联表查询结果。
 *
 * <p>统一承接关注/粉丝分页时的关系信息与用户基础资料，便于交由 MapStruct 转换为 VO。
 */
@Data
public class FollowRelationUserItem {
    private Long relationId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer isSpecialFollow;
    private String remark;
    private Integer mutualFollow;
    private LocalDateTime followTime;
}
