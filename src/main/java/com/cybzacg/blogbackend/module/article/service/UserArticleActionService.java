package com.cybzacg.blogbackend.module.article.service;

/**
 * 用户文章行为服务接口。
 *
 * <p>定义用户文章行为相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface UserArticleActionService {
    void likeArticle(Long articleId);

    void unlikeArticle(Long articleId);
}
