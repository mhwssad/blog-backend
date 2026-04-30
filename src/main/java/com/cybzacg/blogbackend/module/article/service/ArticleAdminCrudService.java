package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;

/**
 * 文章后台 CRUD 服务接口。
 */
public interface ArticleAdminCrudService {

    PageResult<ArticleAdminVO> pageArticles(ArticleAdminPageQuery query);

    ArticleDetailVO getArticle(Long id);

    ArticleDetailVO createArticle(ArticleSaveRequest request);

    ArticleDetailVO updateArticle(Long id, ArticleSaveRequest request);

    void updateStatus(Long id, Integer status);

    void deleteArticle(Long id);
}