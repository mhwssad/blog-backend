package com.cybzacg.blogbackend.dto.domain.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论。
 */
@TableName(value = "sys_comment")
@Data
public class SysComment {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型（article-文章）
     */
    private String targetType;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论图片（JSON 数组）
     */
    private String images;

    /**
     * 评论者ID
     */
    private Long userId;

    /**
     * 根评论ID（顶级评论为 null）
     */
    private Long rootId;

    /**
     * 父评论ID（顶级评论为 null）
     */
    private Long parentId;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 回复数
     */
    private Integer replyCount;

    /**
     * 状态：0-待审核，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
