package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 文章域对内容模块暴露的 facade 实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleContentFacadeServiceImpl implements ArticleContentFacadeService {
    private final BlogArticleRepository blogArticleRepository;
    private final ArticleAccessControlService articleAccessControlService;
    private final ArticleStatusMachine articleStatusMachine;

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogArticle requireAccessibleArticle(Long articleId,
                                                Long userId,
                                                ResultErrorCode notFoundCode,
                                                String notFoundMessage) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        ExceptionThrowerCore.throwBusinessIf(article == null, notFoundCode, notFoundMessage);
        articleAccessControlService.validateArticleAccess(article, userId);
        return article;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogArticle requireInteractableArticle(Long articleId, Long userId, String actionName) {
        BlogArticle article = requireAccessibleArticle(articleId, userId, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        ExceptionThrowerCore.throwBusinessIfNot(
                articleStatusMachine.canInteract(article),
                ResultErrorCode.FORBIDDEN,
                "当前文章状态不允许" + actionName
        );
        return article;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogArticle findAccessiblePublishedArticle(Long articleId, Long userId) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        if (article == null || !articleStatusMachine.isPublishedForNormalUsers(article, java.time.LocalDateTime.now())) {
            return null;
        }
        return articleAccessControlService.canAccessArticle(article, userId) ? article : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustCommentCount(Long articleId, int delta) {
        adjustCounter(articleId, delta, CounterType.COMMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustCollectCount(Long articleId, int delta) {
        adjustCounter(articleId, delta, CounterType.COLLECT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void adjustLikeCount(Long articleId, int delta) {
        adjustCounter(articleId, delta, CounterType.LIKE);
    }

    /**
     * 根据计数类型原子调整文章统计值。
     */
    private void adjustCounter(Long articleId, int delta, CounterType counterType) {
        if (articleId == null || delta == 0) {
            return;
        }
        switch (counterType) {
            case COMMENT -> blogArticleRepository.incrementCommentCount(articleId, delta);
            case COLLECT -> blogArticleRepository.incrementCollectCount(articleId, delta);
            case LIKE -> blogArticleRepository.incrementLikeCount(articleId, delta);
        }
    }

    private enum CounterType {
        COMMENT,
        COLLECT,
        LIKE
    }
}
