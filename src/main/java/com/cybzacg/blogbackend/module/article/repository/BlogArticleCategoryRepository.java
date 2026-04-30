package com.cybzacg.blogbackend.module.article.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.article.BlogArticleCategory;

import java.util.List;

/**
 * 文章分类关联 Repository。<p>封装文章与分类多对多关联关系的持久化操作，提供按文章、分类维度的查询与删除。
 */
public interface BlogArticleCategoryRepository extends IService<BlogArticleCategory> {

    /**
     * 按文章 ID 查询分类关联，按排序字段和 ID 升序返回。
     *
     * @param articleId 文章 ID
     * @return 分类关联列表（按 sortOrder、id 升序）
     */
    List<BlogArticleCategory> listByArticleIdOrdered(Long articleId);

    /**
     * 按分类 ID 查询关联的文章 ID 列表。
     *
     * @param categoryId 分类 ID
     * @return 文章分类关联列表
     */
    List<BlogArticleCategory> listArticleIdsByCategoryId(Long categoryId);

    /**
     * 删除指定文章的全部分类关联。
     *
     * @param articleId 文章 ID
     * @return 是否删除成功
     */
    boolean removeByArticleId(Long articleId);

    /**
     * 判断指定分类下是否关联了文章。
     *
     * @param categoryId 分类 ID
     * @return 是否存在关联
     */
    boolean existsByCategoryId(Long categoryId);
}
