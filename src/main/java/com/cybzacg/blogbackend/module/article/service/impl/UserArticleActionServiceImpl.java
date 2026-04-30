package com.cybzacg.blogbackend.module.article.service.impl;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.content.SysInteraction;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.article.service.UserArticleActionService;
import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ArticleStatusMachine articleStatusMachine;
    private final ContentModelMapper contentModelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 点赞文章，重复点赞时幂等跳过。
     *
     * @param articleId 目标文章 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        BlogArticle article = getInteractableArticle(articleId, userId);
        boolean exists = sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(userId, articleId, "article", "like");
        if (exists) {
            return;
        }
        SysInteraction interaction = contentModelMapper.toInteraction(userId, articleId, "article", "like");
        sysInteractionRepository.save(interaction);
        article.setLikeCount((article.getLikeCount() == null ? 0 : article.getLikeCount()) + 1);
        blogArticleRepository.updateById(article);
        eventPublisher.publishEvent(new XpAwardEvent(
                userId, ExperienceSourceTypeEnum.LIKE_GIVEN.getValue(),
                String.valueOf(articleId),
                "like_given:" + userId + ":" + articleId));
        if (article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
            eventPublisher.publishEvent(new XpAwardEvent(
                    article.getAuthorId(), ExperienceSourceTypeEnum.LIKE_RECEIVED.getValue(),
                    String.valueOf(articleId),
                    "like_received:" + article.getAuthorId() + ":" + articleId));
            notificationDeliveryService.deliverAfterCommit(
                    article.getAuthorId(),
                    NotificationTypeEnum.LIKE_ME,
                    "你的文章收到了点赞",
                    "《" + article.getTitle() + "》收到了新的点赞",
                    userId);
        }
    }

    /**
     * 取消点赞文章，未点赞时幂等跳过。
     *
     * @param articleId 目标文章 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId) {
        Long userId = SecurityUtils.requireUserId();
        SysInteraction interaction = sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(userId, articleId, "article", "like");
        if (interaction == null) {
            return;
        }
        sysInteractionRepository.removeById(interaction.getId());
        BlogArticle article = blogArticleRepository.getById(articleId);
        if (article != null) {
            article.setLikeCount(Math.max(0, (article.getLikeCount() == null ? 0 : article.getLikeCount()) - 1));
            blogArticleRepository.updateById(article);
        }
    }

    /**
     * 获取当前用户可访问且允许新增互动的文章，不满足条件时抛出统一异常。
     */
    private BlogArticle getInteractableArticle(Long articleId, Long userId) {
        BlogArticle article = blogArticleRepository.getById(articleId);
        ExceptionThrowerCore.throwBusinessIf(article == null, ResultErrorCode.ILLEGAL_ARGUMENT, "文章不存在");
        articleAccessControlService.validateArticleAccess(article, userId);
        ExceptionThrowerCore.throwBusinessIfNot(articleStatusMachine.canInteract(article),
                ResultErrorCode.FORBIDDEN, "当前文章状态不允许点赞");
        return article;
    }
}

