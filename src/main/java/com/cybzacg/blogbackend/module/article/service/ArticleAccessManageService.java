package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;

import java.util.List;

/**
 * 文章访问名单管理服务。
 *
 * <p>负责访问名单能力判定、授权项校验和名单重建，供后台与用户侧复用。
 */
public interface ArticleAccessManageService {

    /**
     * 判断文章当前是否允许配置访问名单。
     */
    boolean supportsAccessList(BlogArticle article);

    /**
     * 校验访问授权项是否合法。
     */
    void validateAccessItems(List<ArticleAccessItem> accessList);

    /**
     * 按最新请求重建文章访问名单。
     */
    void rebuildArticleAccessBindings(Long articleId, List<ArticleAccessItem> accessList);
}
