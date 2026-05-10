package com.cybzacg.blogbackend.dto.repository.article.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleSeriesItem;
import com.cybzacg.blogbackend.dto.mapper.article.BlogArticleSeriesItemMapper;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleSeriesItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文章系列关联 Repository 实现。
 */
@Repository
public class BlogArticleSeriesItemRepositoryImpl extends ServiceImpl<BlogArticleSeriesItemMapper, BlogArticleSeriesItem>
        implements BlogArticleSeriesItemRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BlogArticleSeriesItem> listBySeriesIdOrdered(Long seriesId) {
        return list(new LambdaQueryWrapper<BlogArticleSeriesItem>()
                .eq(BlogArticleSeriesItem::getSeriesId, seriesId)
                .orderByAsc(BlogArticleSeriesItem::getSeqNo)
                .orderByAsc(BlogArticleSeriesItem::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BlogArticleSeriesItem> listByArticleId(Long articleId) {
        return list(new LambdaQueryWrapper<BlogArticleSeriesItem>()
                .eq(BlogArticleSeriesItem::getArticleId, articleId)
                .orderByAsc(BlogArticleSeriesItem::getSeriesId)
                .orderByAsc(BlogArticleSeriesItem::getSeqNo)
                .orderByAsc(BlogArticleSeriesItem::getId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsBySeriesIdAndArticleId(Long seriesId, Long articleId) {
        return exists(new LambdaQueryWrapper<BlogArticleSeriesItem>()
                .eq(BlogArticleSeriesItem::getSeriesId, seriesId)
                .eq(BlogArticleSeriesItem::getArticleId, articleId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getMaxSeqNo(Long seriesId) {
        List<BlogArticleSeriesItem> items = list(new LambdaQueryWrapper<BlogArticleSeriesItem>()
                .eq(BlogArticleSeriesItem::getSeriesId, seriesId)
                .orderByDesc(BlogArticleSeriesItem::getSeqNo)
                .last("limit 1"));
        return items.isEmpty() ? null : items.get(0).getSeqNo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeBySeriesId(Long seriesId) {
        remove(new LambdaQueryWrapper<BlogArticleSeriesItem>()
                .eq(BlogArticleSeriesItem::getSeriesId, seriesId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeBySeriesIdAndArticleId(Long seriesId, Long articleId) {
        remove(new LambdaQueryWrapper<BlogArticleSeriesItem>()
                .eq(BlogArticleSeriesItem::getSeriesId, seriesId)
                .eq(BlogArticleSeriesItem::getArticleId, articleId));
    }
}
