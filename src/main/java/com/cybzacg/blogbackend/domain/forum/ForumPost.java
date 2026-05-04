package com.cybzacg.blogbackend.domain.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛帖子。
 */
@Data
@TableName("forum_post")
public class ForumPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sectionId;
    private Long authorId;
    private String title;
    private String content;
    /**
     * 状态：0-草稿，1-已发布，2-审核中，3-已拒绝，4-已删除。
     */
    private Integer status;
    /**
     * 可见范围：0-公开，1-登录可见。
     */
    private Integer visibilityScope;
    private Integer isTop;
    private Integer isEssence;
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    private Integer collectCount;
    private Integer shareCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
