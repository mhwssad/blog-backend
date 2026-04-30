package com.cybzacg.blogbackend.module.article.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.article.BlogArticleReviewLog;
import com.cybzacg.blogbackend.mapper.article.BlogArticleReviewLogMapper;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleReviewLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文章审核日志 Repository 实现。
 */
@Repository
public class BlogArticleReviewLogRepositoryImpl extends ServiceImpl<BlogArticleReviewLogMapper, BlogArticleReviewLog>
        implements BlogArticleReviewLogRepository {

    @Override
    public List<BlogArticleReviewLog> listByArticleId(Long articleId) {
        return list(new LambdaQueryWrapper<BlogArticleReviewLog>()
                .eq(BlogArticleReviewLog::getArticleId, articleId)
                .orderByDesc(BlogArticleReviewLog::getOperatedAt)
                .orderByDesc(BlogArticleReviewLog::getId));
    }
}
