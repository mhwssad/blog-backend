package com.cybzacg.blogbackend.module.article.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系列文章绑定请求。
 */
@Data
@Schema(description = "系列加入文章请求")
public class ArticleSeriesArticleRequest {
    @NotNull(message = "文章ID不能为空")
    @Schema(description = "文章ID")
    private Long articleId;
}
