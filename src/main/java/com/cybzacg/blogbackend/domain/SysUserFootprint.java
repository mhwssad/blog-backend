package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "sys_user_footprint")
@Data
public class SysUserFootprint {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long targetId;

    private String targetType;

    private String title;

    private String url;

    private String ipAddress;

    private String userAgent;

    private Date visitedAt;
}
