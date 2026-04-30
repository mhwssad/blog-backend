package com.cybzacg.blogbackend.module.article.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.article.BlogArticleAccess;
import com.cybzacg.blogbackend.mapper.article.BlogArticleAccessMapper;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleAccessRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文章访问授权 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文章访问授权记录的增删改查。
 */
@Repository
public class BlogArticleAccessRepositoryImpl extends ServiceImpl<BlogArticleAccessMapper, BlogArticleAccess>
        implements BlogArticleAccessRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeByArticleId(Long articleId) {
        return remove(new LambdaQueryWrapper<BlogArticleAccess>()
                .eq(BlogArticleAccess::getArticleId, articleId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BlogArticleAccess> listByArticleIdOrdered(Long articleId) {
        return list(new LambdaQueryWrapper<BlogArticleAccess>()
                .eq(BlogArticleAccess::getArticleId, articleId)
                .orderByAsc(BlogArticleAccess::getAccessType)
                .orderByAsc(BlogArticleAccess::getUserId));
    }
}
