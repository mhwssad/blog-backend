package com.cybzacg.blogbackend.module.article;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.article.service.impl.UserArticleActionServiceImpl;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserArticleActionServiceImplTest {
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private SysInteractionService sysInteractionService;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysInteraction> interactionQuery;

    private UserArticleActionServiceImpl userArticleActionService;

    @BeforeEach
    void setUp() {
        userArticleActionService = new UserArticleActionServiceImpl(
                blogArticleService,
                sysInteractionService,
                articleAccessControlService,
                contentModelMapper
        );
        lenient().when(sysInteractionService.lambdaQuery()).thenReturn(interactionQuery);
        lenient().when(interactionQuery.eq(anySFunction(), any())).thenReturn(interactionQuery);
    }

    @Test
    void likeArticleShouldCreateInteractionAndIncreaseCount() {
        BlogArticle article = publishedArticle(1L, 2);
        SysInteraction interaction = interaction(100L, 7L, 1L);
        when(blogArticleService.getById(1L)).thenReturn(article);
        when(interactionQuery.exists()).thenReturn(false);
        when(contentModelMapper.toInteraction(7L, 1L, "article", "like")).thenReturn(interaction);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.likeArticle(1L);

            assertEquals(3, article.getLikeCount());
            verify(articleAccessControlService).validateArticleAccess(article, 7L);
            verify(sysInteractionService).save(interaction);
            verify(blogArticleService).updateById(article);
        }
    }

    @Test
    void likeArticleShouldBeIdempotentWhenAlreadyLiked() {
        BlogArticle article = publishedArticle(1L, 2);
        when(blogArticleService.getById(1L)).thenReturn(article);
        when(interactionQuery.exists()).thenReturn(true);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.likeArticle(1L);

            assertEquals(2, article.getLikeCount());
            verify(sysInteractionService, never()).save(any(SysInteraction.class));
            verify(blogArticleService, never()).updateById(article);
        }
    }

    @Test
    void likeArticleShouldThrowWhenArticleMissing() {
        when(blogArticleService.getById(1L)).thenReturn(null);

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
        when(blogArticleService.getById(1L)).thenReturn(article);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> userArticleActionService.likeArticle(1L));

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("文章不存在", exception.getMessage());
            verifyNoInteractions(contentModelMapper);
        }
    }

    @Test
    void likeArticleShouldThrowWhenNoAccess() {
        BlogArticle article = publishedArticle(1L, 2);
        when(blogArticleService.getById(1L)).thenReturn(article);
        doThrow(new BusinessException(ResultErrorCode.FORBIDDEN.getCode(), "无权访问"))
                .when(articleAccessControlService).validateArticleAccess(article, 7L);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> userArticleActionService.likeArticle(1L));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertEquals("无权访问", exception.getMessage());
            verify(sysInteractionService, never()).lambdaQuery();
        }
    }

    @Test
    void unlikeArticleShouldRemoveInteractionAndDecreaseCount() {
        BlogArticle article = publishedArticle(1L, 3);
        SysInteraction interaction = interaction(100L, 7L, 1L);
        when(blogArticleService.getById(1L)).thenReturn(article);
        when(interactionQuery.one()).thenReturn(interaction);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.unlikeArticle(1L);

            assertEquals(2, article.getLikeCount());
            verify(sysInteractionService).removeById(100L);
            verify(blogArticleService).updateById(article);
        }
    }

    @Test
    void unlikeArticleShouldBeIdempotentWhenNotLiked() {
        BlogArticle article = publishedArticle(1L, 3);
        when(blogArticleService.getById(1L)).thenReturn(article);
        when(interactionQuery.one()).thenReturn(null);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.unlikeArticle(1L);

            assertEquals(3, article.getLikeCount());
            verify(sysInteractionService, never()).removeById(anyLong());
            verify(blogArticleService, never()).updateById(article);
        }
    }

    @Test
    void unlikeArticleShouldNotDecreaseBelowZero() {
        BlogArticle article = publishedArticle(1L, 0);
        SysInteraction interaction = interaction(100L, 7L, 1L);
        when(blogArticleService.getById(1L)).thenReturn(article);
        when(interactionQuery.one()).thenReturn(interaction);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(7L)) {
            userArticleActionService.unlikeArticle(1L);

            assertEquals(0, article.getLikeCount());
            verify(sysInteractionService).removeById(100L);
            verify(blogArticleService).updateById(article);
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

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
