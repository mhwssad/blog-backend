package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName blog_article_access
 */
@TableName(value ="blog_article_access")
@Data
public class BlogArticleAccess {
    private Long id;

    private Long articleId;

    private Long userId;

    private Integer accessType;

    private Date grantTime;

    private Date expireTime;

    private String grantReason;

    private Date createdAt;

    private Date updatedAt;
}
