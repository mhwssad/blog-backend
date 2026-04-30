package com.cybzacg.blogbackend.module.article.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.article.BlogArticleAccess;

import java.util.List;

/**
 * 文章访问授权 Repository。<p>封装文章访问授权记录的持久化操作，提供按文章维度的查询与删除。
 */
public interface BlogArticleAccessRepository extends IService<BlogArticleAccess> {

    /**
     * 删除指定文章的全部访问授权记录。
     *
     * @param articleId 文章 ID
     * @return 是否删除成功
     */
    boolean removeByArticleId(Long articleId);

    /**
     * 按文章 ID 查询访问授权记录，按访问类型和用户 ID 升序返回。
     *
     * @param articleId 文章 ID
     * @return 访问授权列表
     */
    List<BlogArticleAccess> listByArticleIdOrdered(Long articleId);
}
