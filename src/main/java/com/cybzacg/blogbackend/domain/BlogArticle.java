package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName blog_article
 */
@TableName(value ="blog_article")
@Data
public class BlogArticle {
    private Long id;

    private String title;

    private String summary;

    private String content;

    private String coverImage;

    private Long authorId;

    private Integer isTop;

    private Integer isOriginal;

    private String sourceUrl;

    private Integer status;

    private Date publishTime;

    private Integer accessLevel;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private Integer shareCount;

    private Date createdAt;

    private Date updatedAt;

    private String remark;
}
