package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesSummaryVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesVO;
import com.cybzacg.blogbackend.module.article.model.user.*;

import java.util.List;

/**
 * 文章系列服务。
 */
public interface ArticleSeriesService {

    /**
     * 查询当前用户自己的系列列表。
     */
    List<UserArticleSeriesVO> listMySeries();

    /**
     * 查询当前用户自己的系列详情。
     */
    UserArticleSeriesDetailVO getMySeries(Long id);

    /**
     * 创建系列。
     */
    UserArticleSeriesDetailVO createSeries(ArticleSeriesSaveRequest request);

    /**
     * 更新系列。
     */
    UserArticleSeriesDetailVO updateSeries(Long id, ArticleSeriesSaveRequest request);

    /**
     * 删除系列。
     */
    void deleteSeries(Long id);

    /**
     * 向系列加入文章。
     */
    UserArticleSeriesDetailVO addArticle(Long id, ArticleSeriesArticleRequest request);

    /**
     * 从系列移出文章。
     */
    UserArticleSeriesDetailVO removeArticle(Long id, Long articleId);

    /**
     * 调整系列内文章顺序。
     */
    UserArticleSeriesDetailVO sortArticles(Long id, ArticleSeriesSortRequest request);

    /**
     * 查询作者公开系列列表。
     */
    List<PublicArticleSeriesVO> listAuthorSeries(Long authorId);

    /**
     * 查询公开系列详情。
     */
    PublicArticleSeriesDetailVO getPublicSeries(Long id);

    /**
     * 查询文章所属、且当前用户可见的系列摘要。
     */
    List<ArticleSeriesSummaryVO> listVisibleSeriesSummariesByArticleId(Long articleId, Long userId);

    /**
     * 文章删除或变更归属后清理系列关联。
     */
    void cleanupArticleSeriesRelations(Long articleId);
}
