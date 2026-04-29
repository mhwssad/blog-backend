package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesArticleRequest;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSortRequest;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesDetailVO;

/**
 * 文章系列-文章关联管理服务。
 */
public interface ArticleSeriesItemService {

    /**
     * 向系列加入文章。
     */
    UserArticleSeriesDetailVO addArticle(Long seriesId, ArticleSeriesArticleRequest request);

    /**
     * 从系列移出文章。
     */
    UserArticleSeriesDetailVO removeArticle(Long seriesId, Long articleId);

    /**
     * 调整系列内文章顺序。
     */
    UserArticleSeriesDetailVO sortArticles(Long seriesId, ArticleSeriesSortRequest request);

    /**
     * 文章删除或变更归属后清理系列关联。
     */
    void cleanupArticleSeriesRelations(Long articleId);
}
