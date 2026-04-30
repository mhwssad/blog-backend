package com.cybzacg.blogbackend.domain.follow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注关系表。
 */
@Data
@TableName("sys_user_follow")
public class SysUserFollow {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 关注者ID
     */
    private Long followerId;
    /**
     * 被关注者ID
     */
    private Long followingId;
    /**
     * 关注状态：0-正常，1-已拉黑
     */
    private Integer followStatus;
    /**
     * 是否特别关注：0-否，1-是
     */
    private Integer isSpecialFollow;
    /**
     * 关注来源（search-搜索，recommend-推荐）
     */
    private String source;
    /**
     * 关注时间
     */
    private LocalDateTime followTime;
    /**
     * 取关时间
     */
    private LocalDateTime unfollowTime;
    /**
     * 关注备注
     */
    private String remark;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
