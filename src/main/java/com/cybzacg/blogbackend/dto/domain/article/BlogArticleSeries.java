package com.cybzacg.blogbackend.dto.domain.article;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章系列/专栏表。
 */
@TableName(value = "blog_article_series")
@Data
public class BlogArticleSeries {
    /**
     * 系列ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建人ID
     */
    private Long ownerUserId;

    /**
     * 系列标题
     */
    private String title;

    /**
     * 系列描述
     */
    private String description;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 状态：0-停用，1-正常
     */
    private Integer status;

    /**
     * 可见范围：0-公开，1-仅自己可见，2-白名单可见，3-登录可见
     */
    private Integer visibilityScope;

    /**
     * 文章数
     */
    private Integer articleCount;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
