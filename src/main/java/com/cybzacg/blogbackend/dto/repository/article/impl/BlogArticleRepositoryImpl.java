package com.cybzacg.blogbackend.dto.repository.article.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.dto.mapper.article.BlogArticleMapper;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleRepository;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import com.cybzacg.blogbackend.enums.article.ArticleVisibilityScopeEnum;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 文章 Repository 实现。<p>基于 MyBatis-Plus ServiceImpl 提供文章数据的增删改查。
 */
@Repository
public class BlogArticleRepositoryImpl extends ServiceImpl<BlogArticleMapper, BlogArticle>
        implements BlogArticleRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<BlogArticle> pageAdminArticles(ArticleAdminPageQuery query, Set<Long> filteredArticleIds) {
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<>();
        // 关键字同时匹配标题和摘要
        if (StrUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(BlogArticle::getTitle, query.getKeyword())
                    .or()
                    .like(BlogArticle::getSummary, query.getKeyword()));
        }
        wrapper.eq(query.getAuthorId() != null, BlogArticle::getAuthorId, query.getAuthorId())
                .eq(query.getStatus() != null, BlogArticle::getStatus, query.getStatus())
                .eq(query.getReviewStatus() != null, BlogArticle::getReviewStatus, query.getReviewStatus())
                .eq(query.getAccessLevel() != null, BlogArticle::getAccessLevel, query.getAccessLevel())
                .eq(query.getVisibilityScope() != null, BlogArticle::getVisibilityScope, query.getVisibilityScope())
                .eq(query.getIsTop() != null, BlogArticle::getIsTop, query.getIsTop())
                .eq(query.getIsRecommend() != null, BlogArticle::getIsRecommend, query.getIsRecommend())
                .ge(query.getPublishTimeStart() != null, BlogArticle::getPublishTime, query.getPublishTimeStart())
                .le(query.getPublishTimeEnd() != null, BlogArticle::getPublishTime, query.getPublishTimeEnd())
                .in(filteredArticleIds != null, BlogArticle::getId, filteredArticleIds)
                .orderByDesc(BlogArticle::getUpdatedAt)
                .orderByDesc(BlogArticle::getId);
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<BlogArticle> pagePublishedArticles(PublicArticlePageQuery query, Set<Long> filteredArticleIds) {
        QueryWrapper<BlogArticle> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .in("review_status", 0, 2)
                .eq("visibility_scope", 0)
                .eq("access_level", 0)
                .and(StrUtils.hasText(query.getKeyword()), w -> w.like("title", query.getKeyword())
                        .or()
                        .like("summary", query.getKeyword()))
                .and(filteredArticleIds != null, w -> w.in("id", filteredArticleIds))
                .exists(query.getCategoryId() != null,
                        "select 1 from blog_article_category bac where bac.article_id = blog_article.id and bac.category_id = {0}",
                        query.getCategoryId())
                .exists(query.getTagId() != null,
                        "select 1 from sys_tag_relation str where str.target_id = blog_article.id and str.target_type = 'article' and str.tag_id = {0}",
                        query.getTagId())
                .and(w -> w.isNull("scheduled_publish_time")
                        .or()
                        .le("scheduled_publish_time", LocalDateTime.now()));

        String sort = query.getSort() == null ? "latest" : query.getSort().trim().toLowerCase();
        if ("hot".equals(sort)) {
            wrapper.last("order by (ifnull(view_count, 0) + ifnull(like_count, 0) + ifnull(comment_count, 0)) desc, id desc");
        } else if ("top".equals(sort)) {
            wrapper.last("order by is_top desc, is_recommend desc, publish_time desc, id desc");
        } else {
            wrapper.last("order by is_top desc, is_recommend desc, publish_time desc, id desc");
        }
        return page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BlogArticle> listReadyForScheduledPublish(LocalDateTime now, int limit) {
        int actualLimit = limit <= 0 ? 100 : limit;
        QueryWrapper<BlogArticle> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
                .isNotNull("scheduled_publish_time")
                .le("scheduled_publish_time", now)
                .last("order by scheduled_publish_time asc, id asc limit " + actualLimit);
        return list(wrapper);
    }

    @Override
    public List<BlogArticle> listPublicVisibleForRag(int limit) {
        int actualLimit = limit <= 0 ? 1000 : limit;
        return list(publicVisibleRagWrapper()
                .orderByDesc(BlogArticle::getUpdatedAt)
                .last("limit " + actualLimit));
    }

    @Override
    public BlogArticle findPublicVisibleForRag(Long articleId) {
        if (articleId == null) {
            return null;
        }
        return getOne(publicVisibleRagWrapper()
                .eq(BlogArticle::getId, articleId)
                .last("limit 1"), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByAuthorId(Long authorId) {
        return count(new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getAuthorId, authorId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countPublicVisibleByAuthorId(Long authorId) {
        return count(new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getAuthorId, authorId)
                .eq(BlogArticle::getStatus, 1)
                .in(BlogArticle::getReviewStatus,
                        ArticleReviewStatusEnum.NOT_SUBMITTED.getValue(),
                        ArticleReviewStatusEnum.APPROVED.getValue())
                .eq(BlogArticle::getVisibilityScope, ArticleVisibilityScopeEnum.PUBLIC.getValue())
                .eq(BlogArticle::getAccessLevel, 0)
                .and(wrapper -> wrapper.isNull(BlogArticle::getScheduledPublishTime)
                        .or()
                        .le(BlogArticle::getScheduledPublishTime, LocalDateTime.now())));
    }

    private LambdaQueryWrapper<BlogArticle> publicVisibleRagWrapper() {
        return new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getStatus, 1)
                .in(BlogArticle::getReviewStatus,
                        ArticleReviewStatusEnum.NOT_SUBMITTED.getValue(),
                        ArticleReviewStatusEnum.APPROVED.getValue())
                .eq(BlogArticle::getVisibilityScope, ArticleVisibilityScopeEnum.PUBLIC.getValue())
                .eq(BlogArticle::getAccessLevel, 0)
                .and(wrapper -> wrapper.isNull(BlogArticle::getScheduledPublishTime)
                        .or()
                        .le(BlogArticle::getScheduledPublishTime, LocalDateTime.now()));
    }

    @Override
    public void incrementLikeCount(Long id, int delta) {
        baseMapper.incrementLikeCount(id, delta);
    }

    @Override
    public void incrementCommentCount(Long id, int delta) {
        baseMapper.incrementCommentCount(id, delta);
    }

    @Override
    public void incrementCollectCount(Long id, int delta) {
        baseMapper.incrementCollectCount(id, delta);
    }
}
