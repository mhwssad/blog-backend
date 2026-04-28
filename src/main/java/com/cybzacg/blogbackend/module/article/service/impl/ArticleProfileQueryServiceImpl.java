package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.module.article.model.internal.AuthorPublicProfileStats;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleSeriesRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 文章域对外资料查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArticleProfileQueryServiceImpl implements ArticleProfileQueryService {
    private final BlogArticleRepository blogArticleRepository;
    private final BlogArticleSeriesRepository blogArticleSeriesRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorPublicProfileStats getAuthorPublicProfileStats(Long authorId) {
        if (authorId == null) {
            return new AuthorPublicProfileStats(0L, 0L);
        }
        return new AuthorPublicProfileStats(
                blogArticleRepository.countPublicVisibleByAuthorId(authorId),
                blogArticleSeriesRepository.countPublicVisibleByOwnerUserId(authorId));
    }
}
