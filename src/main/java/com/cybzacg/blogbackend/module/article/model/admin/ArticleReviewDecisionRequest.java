package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章审核决策请求。
 */
@Data
@Schema(description = "文章审核决策请求")
public class ArticleReviewDecisionRequest {
    @Size(max = 512, message = "审核说明长度不能超过512个字符")
    @Schema(description = "审核说明")
    private String reviewComment;
}
