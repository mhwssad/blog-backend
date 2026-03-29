package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户关注关系表。
 */
@Data
@TableName("sys_user_follow")
public class SysUserFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long followerId;
    private Long followingId;
    private Integer followStatus;
    private Integer isSpecialFollow;
    private String source;
    private Date followTime;
    private Date unfollowTime;
    private String remark;
    private Date createdAt;
    private Date updatedAt;
}
