package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户浏览足迹。 */
@TableName(value = "sys_user_footprint")
@Data
public class SysUserFootprint {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 浏览目标ID */
    private Long targetId;

    /** 浏览目标类型（article-文章） */
    private String targetType;

    /** 浏览目标标题 */
    private String title;

    /** 浏览目标URL */
    private String url;

    /** 访问IP地址 */
    private String ipAddress;

    /** 用户浏览器UA */
    private String userAgent;

    /** 访问时间 */
    private LocalDateTime visitedAt;
}
