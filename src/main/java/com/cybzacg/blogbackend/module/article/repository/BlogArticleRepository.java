package com.cybzacg.blogbackend.module.article.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;

import java.util.List;
import java.util.Set;

/**
 * 文章 Repository。
 */
public interface BlogArticleRepository extends IService<BlogArticle> {

    /**
     * 按后台条件分页查询文章。
     *
     * @param query 查询条件
     * @param filteredArticleIds 分类/标签反查后的文章 ID 集合，为 null 时不做 ID 过滤
     * @return 文章分页结果
     */
    Page<BlogArticle> pageAdminArticles(ArticleAdminPageQuery query, Set<Long> filteredArticleIds);

    /**
     * 查询所有已发布文章。
     *
     * @return 已发布文章列表
     */
    List<BlogArticle> listAllPublished();
}
