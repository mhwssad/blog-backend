package com.cybzacg.blogbackend.dto.domain.article;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章分类关联表。
 */
@TableName(value = "blog_article_category")
@Data
public class BlogArticleCategory {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 文章ID
     */
    private Long articleId;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 排序值
     */
    private Integer sortOrder;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
