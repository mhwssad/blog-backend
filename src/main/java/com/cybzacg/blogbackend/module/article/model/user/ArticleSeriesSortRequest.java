package com.cybzacg.blogbackend.module.article.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 系列文章排序请求。
 */
@Data
@Schema(description = "系列文章排序请求")
public class ArticleSeriesSortRequest {
    @NotEmpty(message = "排序后的文章ID列表不能为空")
    @Schema(description = "按目标顺序排列的文章ID列表")
    private List<Long> articleIds;
}
