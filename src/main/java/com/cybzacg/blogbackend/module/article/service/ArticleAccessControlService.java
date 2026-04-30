package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleAccess;

import java.util.List;

/**
 * 文章访问控制服务接口。
 *
 * <p>定义文章访问控制相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface ArticleAccessControlService {
    boolean canAccessArticle(BlogArticle article, Long userId);

    void validateArticleAccess(BlogArticle article, Long userId);

    boolean hasArticleAccess(Long articleId, Long userId);

    List<BlogArticleAccess> listArticleAccesses(Long articleId);
}
