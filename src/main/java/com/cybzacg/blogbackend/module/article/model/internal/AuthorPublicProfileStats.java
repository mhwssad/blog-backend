package com.cybzacg.blogbackend.module.article.model.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 作者公开主页统计摘要。
 */
@Data
@AllArgsConstructor
public class AuthorPublicProfileStats {
    private Long publicArticleCount;

    private Long publicSeriesCount;
}
