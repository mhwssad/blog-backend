package com.cybzacg.blogbackend.module.article.service;

/**
 * 用户文章行为服务接口。
 *
 * <p>定义用户文章行为相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface UserArticleActionService {
    /**
     * 为当前登录用户点赞文章，并同步维护文章点赞计数。
     */
    void likeArticle(Long articleId);

    /**
     * 为当前登录用户取消点赞文章，并同步维护文章点赞计数。
     */
    void unlikeArticle(Long articleId);
}
