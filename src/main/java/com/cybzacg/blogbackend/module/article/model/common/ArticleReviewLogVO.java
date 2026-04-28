package com.cybzacg.blogbackend.module.article.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章审核日志视图。
 */
@Data
@Schema(description = "文章审核日志")
public class ArticleReviewLogVO {
    @Schema(description = "日志ID")
    private Long id;
    @Schema(description = "文章ID")
    private Long articleId;
    @Schema(description = "审核动作")
    private String actionType;
    @Schema(description = "审核动作标签")
    private String actionTypeLabel;
    @Schema(description = "变更前审核状态")
    private Integer fromReviewStatus;
    @Schema(description = "变更前审核状态标签")
    private String fromReviewStatusLabel;
    @Schema(description = "变更后审核状态")
    private Integer toReviewStatus;
    @Schema(description = "变更后审核状态标签")
    private String toReviewStatusLabel;
    @Schema(description = "操作人ID")
    private Long operatorUserId;
    @Schema(description = "操作人用户名")
    private String operatorUsername;
    @Schema(description = "操作人昵称")
    private String operatorNickname;
    @Schema(description = "审核说明/备注")
    private String reviewComment;
    @Schema(description = "操作时间")
    private LocalDateTime operatedAt;
}
