package com.cybzacg.blogbackend.domain.article;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章表。
 */
@TableName(value = "blog_article")
@Data
public class BlogArticle {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 文章标题
     */
    private String title;
    /**
     * 文章摘要
     */
    private String summary;
    /**
     * 文章内容（Markdown格式）
     */
    private String content;
    /**
     * 封面图片URL
     */
    private String coverImage;
    /**
     * 作者ID
     */
    private Long authorId;
    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;
    /**
     * 是否推荐：0-否，1-是
     */
    private Integer isRecommend;
    /**
     * 是否原创：0-转载，1-原创
     */
    private Integer isOriginal;
    /**
     * 转载来源URL（原创时为空）
     */
    private String sourceUrl;
    /**
     * 状态：0-草稿，1-已发布，2-已下架
     */
    private Integer status;
    /**
     * 审核状态：0-未送审/免审，1-审核中，2-审核通过，3-审核拒绝
     */
    private Integer reviewStatus;
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    /**
     * 定时发布时间
     */
    private LocalDateTime scheduledPublishTime;
    /**
     * 访问级别：0-公开，1-登录可见，2-密码访问，3-指定用户
     */
    private Integer accessLevel;
    /**
     * 可见范围：0-公开，1-仅自己可见，2-白名单可见，3-登录可见
     */
    private Integer visibilityScope;
    /**
     * 浏览数
     */
    private Integer viewCount;
    /**
     * 点赞数
     */
    private Integer likeCount;
    /**
     * 评论数
     */
    private Integer commentCount;
    /**
     * 收藏数
     */
    private Integer collectCount;
    /**
     * 分享数
     */
    private Integer shareCount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 备注
     */
    private String remark;
}
