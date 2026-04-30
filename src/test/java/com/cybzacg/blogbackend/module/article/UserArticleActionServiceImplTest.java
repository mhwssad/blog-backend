package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.article.service.impl.UserArticleActionServiceImpl;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserArticleActionServiceImplTest {
    @Mock
    private BlogArticleRepository blogArticleRepository;
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ArticleStatusMachine articleStatusMachine;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    private UserArticleActionServiceImpl userArticleActionService;

    @BeforeEach
    void setUp() {
        userArticleActionService = new UserArticleActionServiceImpl(
                blogArticleRepository,
                sysInteractionRepository,
                articleAccessControlService,
                articleStatusMachine,
                contentModelMapper,
                eventPublisher,
                notificationDeliveryService
        );
        lenient().when(articleStatusMachine.canInteract(any())).thenReturn(true);
    }

    @Test
    void likeArticleShouldCreateInteractionAndIncreaseCount() {
        BlogArticle article = publishedArticle(1L, 2);
        SysInteraction interaction = interaction(100L, 7L, 1L);
        when(blogArticleRepository.getById(1L)).thenReturn(article);
        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 1L, "article", "like")).thenReturn(false);
        when(contentModelMapper.toInteraction(7L, 1L, "article", "like")).thenReturn(interaction);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.likeArticle(1L);

            assertEquals(3, article.getLikeCount());
            verify(articleAccessControlService).validateArticleAccess(article, 7L);
            verify(sysInteractionRepository).save(interaction);
            verify(blogArticleRepository).updateById(article);
        }
    }

    @Test
    void likeArticleShouldBeIdempotentWhenAlreadyLiked() {
        BlogArticle article = publishedArticle(1L, 2);
        when(blogArticleRepository.getById(1L)).thenReturn(article);
        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 1L, "article", "like")).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.likeArticle(1L);

            assertEquals(2, article.getLikeCount());
            verify(sysInteractionRepository, never()).save(any(SysInteraction.class));
            verify(blogArticleRepository, never()).updateById(article);
        }
    }

    @Test
    void likeArticleShouldThrowWhenArticleMissing() {
        when(blogArticleRepository.getById(1L)).thenReturn(null);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> userArticleActionService.likeArticle(1L));

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("文章不存在", exception.getMessage());
            verifyNoInteractions(articleAccessControlService, contentModelMapper);
        }
    }

    @Test
    void likeArticleShouldThrowWhenArticleUnpublished() {
        BlogArticle article = publishedArticle(1L, 2);
        article.setStatus(0);
        when(blogArticleRepository.getById(1L)).thenReturn(article);
        when(articleStatusMachine.canInteract(article)).thenReturn(false);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> userArticleActionService.likeArticle(1L));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertEquals("当前文章状态不允许点赞", exception.getMessage());
            verifyNoInteractions(contentModelMapper);
        }
    }

    @Test
    void likeArticleShouldThrowWhenNoAccess() {
        BlogArticle article = publishedArticle(1L, 2);
        when(blogArticleRepository.getById(1L)).thenReturn(article);
        doThrow(new BusinessException(ResultErrorCode.FORBIDDEN.getCode(), "无权访问"))
                .when(articleAccessControlService).validateArticleAccess(article, 7L);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> userArticleActionService.likeArticle(1L));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertEquals("无权访问", exception.getMessage());
            verify(sysInteractionRepository, never()).existsByUserIdAndTargetIdAndTargetTypeAndActionType(anyLong(), anyLong(), anyString(), anyString());
        }
    }

    @Test
    void unlikeArticleShouldRemoveInteractionAndDecreaseCount() {
        BlogArticle article = publishedArticle(1L, 3);
        SysInteraction interaction = interaction(100L, 7L, 1L);
        when(blogArticleRepository.getById(1L)).thenReturn(article);
        when(sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 1L, "article", "like")).thenReturn(interaction);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.unlikeArticle(1L);

            assertEquals(2, article.getLikeCount());
            verify(sysInteractionRepository).removeById(100L);
            verify(blogArticleRepository).updateById(article);
        }
    }

    @Test
    void unlikeArticleShouldBeIdempotentWhenNotLiked() {
        when(sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 1L, "article", "like")).thenReturn(null);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.unlikeArticle(1L);

            verify(sysInteractionRepository, never()).removeById(anyLong());
            verify(blogArticleRepository, never()).updateById(any());
        }
    }

    @Test
    void unlikeArticleShouldNotDecreaseBelowZero() {
        BlogArticle article = publishedArticle(1L, 0);
        SysInteraction interaction = interaction(100L, 7L, 1L);
        when(blogArticleRepository.getById(1L)).thenReturn(article);
        when(sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 1L, "article", "like")).thenReturn(interaction);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.unlikeArticle(1L);

            assertEquals(0, article.getLikeCount());
            verify(sysInteractionRepository).removeById(100L);
            verify(blogArticleRepository).updateById(article);
        }
    }

    private BlogArticle publishedArticle(Long id, Integer likeCount) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setStatus(1);
        article.setLikeCount(likeCount);
        return article;
    }

    private SysInteraction interaction(Long id, Long userId, Long targetId) {
        SysInteraction interaction = new SysInteraction();
        interaction.setId(id);
        interaction.setUserId(userId);
        interaction.setTargetId(targetId);
        interaction.setTargetType("article");
        interaction.setActionType("like");
        return interaction;
    }
}
