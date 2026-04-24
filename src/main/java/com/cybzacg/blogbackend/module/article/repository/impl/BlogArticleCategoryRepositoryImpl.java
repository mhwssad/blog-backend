package com.cybzacg.blogbackend.module.article.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.mapper.BlogArticleCategoryMapper;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文章分类关联 Repository 实现。
 */
@Repository
public class BlogArticleCategoryRepositoryImpl extends ServiceImpl<BlogArticleCategoryMapper, BlogArticleCategory>
        implements BlogArticleCategoryRepository {

    @Override
    public List<BlogArticleCategory> listByArticleIdOrdered(Long articleId) {
        return list(new LambdaQueryWrapper<BlogArticleCategory>()
                .eq(BlogArticleCategory::getArticleId, articleId)
                .orderByAsc(BlogArticleCategory::getSortOrder)
                .orderByAsc(BlogArticleCategory::getId));
    }

    @Override
    public List<BlogArticleCategory> listArticleIdsByCategoryId(Long categoryId) {
        return list(new LambdaQueryWrapper<BlogArticleCategory>()
                .eq(BlogArticleCategory::getCategoryId, categoryId));
    }

    @Override
    public boolean removeByArticleId(Long articleId) {
        return remove(new LambdaQueryWrapper<BlogArticleCategory>()
                .eq(BlogArticleCategory::getArticleId, articleId));
    }
}
