package com.cybzacg.blogbackend.dto.domain.article;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章系列关联表。
 */
@TableName(value = "blog_article_series_item")
@Data
public class BlogArticleSeriesItem {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 系列ID
     */
    private Long seriesId;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 系列内顺序
     */
    private Integer seqNo;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
