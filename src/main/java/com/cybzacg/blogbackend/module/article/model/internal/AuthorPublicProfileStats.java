package com.cybzacg.blogbackend.module.article.model.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 作者公开主页统计摘要。
 */
@Data
@AllArgsConstructor
@Schema(description = "作者公开主页统计摘要")
public class AuthorPublicProfileStats {
    @Schema(description = "公开文章数")
    private Long publicArticleCount;

    @Schema(description = "公开专栏数")
    private Long publicSeriesCount;
}
