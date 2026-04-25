package com.cybzacg.blogbackend.module.follow.model.data;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公开关注/粉丝列表联表查询结果。
 */
@Data
public class PublicFollowUserItem {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private LocalDateTime followTime;
}
