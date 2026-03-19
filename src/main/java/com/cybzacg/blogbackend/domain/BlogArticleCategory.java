package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName blog_article_category
 */
@TableName(value ="blog_article_category")
@Data
public class BlogArticleCategory {
    private Long id;

    private Long articleId;

    private Long categoryId;

    private Integer sortOrder;

    private Date createdAt;
}
