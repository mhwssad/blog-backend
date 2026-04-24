package com.cybzacg.blogbackend.module.article.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.mapper.BlogArticleMapper;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * 文章 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文章数据的增删改查。
 */
@Repository
public class BlogArticleRepositoryImpl extends ServiceImpl<BlogArticleMapper, BlogArticle>
        implements BlogArticleRepository {

    /** {@inheritDoc} */
    @Override
    public Page<BlogArticle> pageAdminArticles(ArticleAdminPageQuery query, Set<Long> filteredArticleIds) {
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<>();
        // 关键字同时匹配标题和摘要
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(BlogArticle::getTitle, query.getKeyword())
                    .or()
                    .like(BlogArticle::getSummary, query.getKeyword()));
        }
        wrapper.eq(query.getAuthorId() != null, BlogArticle::getAuthorId, query.getAuthorId())
                .eq(query.getStatus() != null, BlogArticle::getStatus, query.getStatus())
                .eq(query.getAccessLevel() != null, BlogArticle::getAccessLevel, query.getAccessLevel())
                .eq(query.getIsTop() != null, BlogArticle::getIsTop, query.getIsTop())
                .ge(query.getPublishTimeStart() != null, BlogArticle::getPublishTime, query.getPublishTimeStart())
                .le(query.getPublishTimeEnd() != null, BlogArticle::getPublishTime, query.getPublishTimeEnd())
                .in(filteredArticleIds != null, BlogArticle::getId, filteredArticleIds)
                .orderByDesc(BlogArticle::getUpdatedAt)
                .orderByDesc(BlogArticle::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    /** {@inheritDoc} */
    @Override
    public List<BlogArticle> listAllPublished() {
        return list(new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getStatus, 1));
    }
}
