package com.cybzacg.blogbackend.domain.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛版块。
 */
@Data
@TableName("forum_section")
public class ForumSection {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 版块名称。
     */
    private String name;
    /**
     * 版块简介。
     */
    private String description;
    /**
     * 展示排序，数值越小越靠前。
     */
    private Integer sortOrder;
    /**
     * 可见范围：0-公开，1-登录可见。
     */
    private Integer visibilityScope;
    /**
     * 发帖最低等级。
     */
    private Integer postLevelLimit;
    /**
     * 状态：0-禁用，1-启用。
     */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
