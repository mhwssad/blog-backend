package com.cybzacg.blogbackend.module.article.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章送审请求。
 */
@Data
@Schema(description = "文章送审请求")
public class ArticleReviewSubmitRequest {
    @Size(max = 512, message = "送审说明长度不能超过512个字符")
    @Schema(description = "送审说明/补充说明")
    private String reviewComment;
}
