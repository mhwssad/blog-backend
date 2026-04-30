package com.cybzacg.blogbackend.domain.article;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章访问授权表。
 */
@TableName(value = "blog_article_access")
@Data
public class BlogArticleAccess {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 文章ID
     */
    private Long articleId;
    /**
     * 授权用户ID
     */
    private Long userId;
    /**
     * 授权类型：1-密码访问，2-指定用户
     */
    private Integer accessType;
    /**
     * 授权时间
     */
    private LocalDateTime grantTime;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 授权原因
     */
    private String grantReason;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
