package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.article.service.UserArticleActionService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户文章互动服务实现。
 *
 * <p>负责文章点赞与取消点赞，并同步维护文章点赞计数。
 */
@Service
@RequiredArgsConstructor
public class UserArticleActionServiceImpl implements UserArticleActionService {
    private final BlogArticleService blogArticleService;
    private final SysInteractionService sysInteractionService;
    private final ArticleAccessControlService articleAccessControlService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = getAccessibleArticle(articleId, userId);
        boolean exists = sysInteractionService.lambdaQuery()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, articleId)
                .eq(SysInteraction::getTargetType, "article")
                .eq(SysInteraction::getActionType, "like")
                .exists();
        if (exists) {
            return;
        }
        SysInteraction interaction = new SysInteraction();
        interaction.setUserId(userId);
        interaction.setTargetId(articleId);
        interaction.setTargetType("article");
        interaction.setActionType("like");
        sysInteractionService.save(interaction);
        article.setLikeCount((article.getLikeCount() == null ? 0 : article.getLikeCount()) + 1);
        blogArticleService.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = getAccessibleArticle(articleId, userId);
        SysInteraction interaction = sysInteractionService.lambdaQuery()
                .eq(SysInteraction::getUserId, userId)
                .eq(SysInteraction::getTargetId, articleId)
                .eq(SysInteraction::getTargetType, "article")
                .eq(SysInteraction::getActionType, "like")
                .one();
        if (interaction == null) {
            return;
        }
        sysInteractionService.removeById(interaction.getId());
        article.setLikeCount(Math.max(0, (article.getLikeCount() == null ? 0 : article.getLikeCount()) - 1));
        blogArticleService.updateById(article);
    }

    /**
     * 获取当前用户可访问的已发布文章，不满足条件时抛出统一异常。
     */
    private BlogArticle getAccessibleArticle(Long articleId, Long userId) {
        BlogArticle article = blogArticleService.getById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "文章不存在");
        }
        articleAccessControlService.validateArticleAccess(article, userId);
        return article;
    }
}
