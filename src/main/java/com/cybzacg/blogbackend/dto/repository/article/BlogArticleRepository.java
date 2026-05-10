package com.cybzacg.blogbackend.dto.repository.article;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticle;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 文章 Repository。<p>封装文章表的持久化操作，提供后台多条件分页查询与已发布文章列表查询。
 */
public interface BlogArticleRepository extends IService<BlogArticle> {

    /**
     * 按后台条件分页查询文章。
     *
     * @param query              查询条件
     * @param filteredArticleIds 分类/标签反查后的文章 ID 集合，为 null 时不做 ID 过滤
     * @return 文章分页结果
     */
    Page<BlogArticle> pageAdminArticles(ArticleAdminPageQuery query, Set<Long> filteredArticleIds);

    Page<BlogArticle> pagePublishedArticles(PublicArticlePageQuery query, Set<Long> filteredArticleIds);

    List<BlogArticle> listReadyForScheduledPublish(LocalDateTime now, int limit);

    /**
     * 查询可进入 RAG 知识库的公开文章。
     */
    List<BlogArticle> listPublicVisibleForRag(int limit);

    /**
     * 查询指定文章，仅当其可进入 RAG 知识库时返回。
     */
    BlogArticle findPublicVisibleForRag(Long articleId);

    long countByAuthorId(Long authorId);

    /**
     * 统计指定作者当前可公开展示的文章数量。
     */
    long countPublicVisibleByAuthorId(Long authorId);

    void incrementLikeCount(Long id, int delta);

    void incrementCommentCount(Long id, int delta);

    void incrementCollectCount(Long id, int delta);
}
