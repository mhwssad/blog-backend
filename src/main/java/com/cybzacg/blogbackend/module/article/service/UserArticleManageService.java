package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticlePageQuery;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleVO;

import java.util.List;

/**
 * 用户侧文章管理服务。
 */
public interface UserArticleManageService {

    /**
     * 分页查询当前登录用户自己的文章。
     */
    PageResult<UserArticleVO> pageMyArticles(UserArticlePageQuery query);

    /**
     * 查询当前登录用户自己的文章详情。
     */
    UserArticleDetailVO getMyArticle(Long id);

    /**
     * 配置当前登录用户自己文章的访问名单。
     */
    void assignMyArticleAccess(Long id, List<ArticleAccessItem> accessList);
}
