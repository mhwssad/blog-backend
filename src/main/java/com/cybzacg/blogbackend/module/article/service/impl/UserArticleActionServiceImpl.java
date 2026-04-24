package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.UserArticleActionService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.repository.SysInteractionRepository;
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
    private final BlogArticleRepository blogArticleRepository;
    private final SysInteractionRepository sysInteractionRepository;
    private final ArticleAccessControlService articleAccessControlService;
    private final ContentModelMapper contentModelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = getAccessibleArticle(articleId, userId);
        boolean exists = sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(userId, articleId, "article", "like");
        if (exists) {
            return;
        }
        SysInteraction interaction = contentModelMapper.toInteraction(userId, articleId, "article", "like");
        sysInteractionRepository.save(interaction);
        article.setLikeCount((article.getLikeCount() == null ? 0 : article.getLikeCount()) + 1);
        blogArticleRepository.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = getAccessibleArticle(articleId, userId);
        SysInteraction interaction = sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(userId, articleId, "article", "like");
        if (interaction == null) {
            return;
        }
        sysInteractionRepository.removeById(interaction.getId());
        article.setLikeCount(Math.max(0, (article.getLikeCount() == null ? 0 : article.getLikeCount()) - 1));
        blogArticleRepository.updateById(article);
    }

    /**
     * 获取当前用户可访问的已发布文章，不满足条件时抛出统一异常。
     */
    private BlogArticle getAccessibleArticle(Long articleId, Long userId) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        ExceptionThrowerCore.throwBusinessIf(article == null || !Integer.valueOf(1).equals(article.getStatus()), ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        articleAccessControlService.validateArticleAccess(article, userId);
        return article;
    }
}



