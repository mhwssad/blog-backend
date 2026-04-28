package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章审核记录表。
 */
@TableName(value = "blog_article_review_log")
@Data
public class BlogArticleReviewLog {
    /**
     * 审核记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 审核动作：submit/resubmit/approve/reject
     */
    private String actionType;

    /**
     * 变更前审核状态：0-未送审，1-审核中，2-审核通过，3-审核拒绝
     */
    private Integer fromReviewStatus;

    /**
     * 变更后审核状态：0-未送审，1-审核中，2-审核通过，3-审核拒绝
     */
    private Integer toReviewStatus;

    /**
     * 操作人ID
     */
    private Long operatorUserId;

    /**
     * 审核说明/备注
     */
    private String reviewComment;

    /**
     * 操作时间
     */
    private LocalDateTime operatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
