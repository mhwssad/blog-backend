package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;

import java.util.List;

/**
 * 文章后台管理服务接口。
 *
 * <p>定义文章后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface ArticleAdminService {
    PageResult<ArticleAdminVO> pageArticles(ArticleAdminPageQuery query);

    ArticleDetailVO getArticle(Long id);

    ArticleDetailVO createArticle(ArticleSaveRequest request);

    ArticleDetailVO updateArticle(Long id, ArticleSaveRequest request);

    void updateStatus(Long id, Integer status);

    void assignAccess(Long id, List<ArticleAccessItem> accessList);

    void deleteArticle(Long id);
}
