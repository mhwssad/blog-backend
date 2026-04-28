package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;

/**
 * 文章域对内容模块暴露的稳定 facade。
 */
public interface ArticleContentFacadeService {

    /**
     * 校验文章存在且当前用户可访问。
     */
    BlogArticle requireAccessibleArticle(Long articleId, Long userId, ResultErrorCode notFoundCode, String notFoundMessage);

    /**
     * 校验文章存在、当前用户可访问且允许互动。
     */
    BlogArticle requireInteractableArticle(Long articleId, Long userId, String actionName);

    /**
     * 查询已发布且当前用户可访问的文章；不满足条件时返回 null。
     */
    BlogArticle findAccessiblePublishedArticle(Long articleId, Long userId);

    /**
     * 调整文章评论数，缺失文章时保持幂等。
     */
    void adjustCommentCount(Long articleId, int delta);

    /**
     * 调整文章收藏数，缺失文章时保持幂等。
     */
    void adjustCollectCount(Long articleId, int delta);

    /**
     * 调整文章点赞数，缺失文章时保持幂等。
     */
    void adjustLikeCount(Long articleId, int delta);
}
