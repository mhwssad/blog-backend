package com.cybzacg.blogbackend.module.article.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.BlogArticleSeries;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.mapper.BlogArticleSeriesMapper;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleSeriesRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文章系列 Repository 实现。
 */
@Repository
public class BlogArticleSeriesRepositoryImpl extends ServiceImpl<BlogArticleSeriesMapper, BlogArticleSeries>
        implements BlogArticleSeriesRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BlogArticleSeries> listByOwnerUserId(Long ownerUserId) {
        return list(new LambdaQueryWrapper<BlogArticleSeries>()
                .eq(BlogArticleSeries::getOwnerUserId, ownerUserId)
                .orderByAsc(BlogArticleSeries::getSortOrder)
                .orderByAsc(BlogArticleSeries::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countPublicVisibleByOwnerUserId(Long ownerUserId) {
        return count(new LambdaQueryWrapper<BlogArticleSeries>()
                .eq(BlogArticleSeries::getOwnerUserId, ownerUserId)
                .eq(BlogArticleSeries::getStatus, 1)
                .eq(BlogArticleSeries::getVisibilityScope, ArticleVisibilityScopeEnum.PUBLIC.getValue()));
    }
}
