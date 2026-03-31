package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.user.CommentSaveRequest;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.impl.UserCommentServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommentServiceImplTest {
    @Mock
    private SysCommentService sysCommentService;
    @Mock
    private SysInteractionService sysInteractionService;
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysComment> commentTreeQuery;
    @Mock
    private LambdaQueryChainWrapper<SysInteraction> interactionExistsQuery;
    @Mock
    private LambdaQueryChainWrapper<SysInteraction> interactionLookupQuery;

    private UserCommentServiceImpl userCommentService;

    @BeforeEach
    void setUp() {
        userCommentService = new UserCommentServiceImpl(
                sysCommentService,
                sysInteractionService,
                blogArticleService,
                articleAccessControlService,
                contentModelMapper
        );
    }

    @Test
    void createCommentShouldUpdateParentReplyCountAndArticleCommentCount() {
        CommentSaveRequest request = new CommentSaveRequest();
        request.setTargetType("article");
        request.setTargetId(10L);
        request.setContent("reply");
        request.setParentId(100L);
        request.setRootId(0L);

        BlogArticle article = new BlogArticle();
        article.setId(10L);
        article.setStatus(1);
        article.setCommentCount(2);

        SysComment parent = new SysComment();
        parent.setId(100L);
        parent.setTargetId(10L);
        parent.setTargetType("article");
        parent.setRootId(0L);
        parent.setReplyCount(1);

        SysComment comment = new SysComment();

        when(blogArticleService.getById(10L)).thenReturn(article);
        when(contentModelMapper.toComment(request)).thenReturn(comment);
        when(sysCommentService.getById(100L)).thenReturn(parent);
        when(sysCommentService.save(comment)).thenAnswer(invocation -> {
            comment.setId(101L);
            return true;
        });
        when(sysCommentService.updateById(parent)).thenReturn(true);
        when(blogArticleService.updateById(article)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.createComment(request);
        }

        assertEquals("article", comment.getTargetType());
        assertEquals(Long.valueOf(7L), comment.getUserId());
        assertEquals(Long.valueOf(100L), comment.getParentId());
        assertEquals(Long.valueOf(100L), comment.getRootId());
        assertEquals(Integer.valueOf(0), comment.getLikeCount());
        assertEquals(Integer.valueOf(0), comment.getReplyCount());
        assertEquals(Integer.valueOf(1), comment.getStatus());
        assertEquals(Integer.valueOf(2), parent.getReplyCount());
        assertEquals(Integer.valueOf(3), article.getCommentCount());
        verify(sysCommentService).save(comment);
        verify(sysCommentService).updateById(parent);
        verify(blogArticleService).updateById(article);
    }

    @Test
    void createCommentShouldRejectWhenArticleAccessDenied() {
        CommentSaveRequest request = new CommentSaveRequest();
        request.setTargetType("article");
        request.setTargetId(10L);
        request.setContent("blocked");

        BlogArticle article = new BlogArticle();
        article.setId(10L);
        article.setStatus(1);

        when(blogArticleService.getById(10L)).thenReturn(article);
        doThrow(new BusinessException(ResultErrorCode.FORBIDDEN))
                .when(articleAccessControlService)
                .validateArticleAccess(article, 7L);

        BusinessException exception;
        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            exception = assertThrows(BusinessException.class, () -> userCommentService.createComment(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        verify(sysCommentService, never()).save(any(SysComment.class));
        verify(blogArticleService, never()).updateById(any(BlogArticle.class));
    }

    @Test
    void deleteCommentShouldRemoveSubtreeInteractionsAndRollbackCounts() {
        SysComment parent = new SysComment();
        parent.setId(50L);
        parent.setTargetId(10L);
        parent.setTargetType("article");
        parent.setParentId(0L);
        parent.setReplyCount(2);

        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setUserId(7L);
        comment.setTargetId(10L);
        comment.setTargetType("article");
        comment.setParentId(50L);

        SysComment child = new SysComment();
        child.setId(101L);
        child.setUserId(8L);
        child.setTargetId(10L);
        child.setTargetType("article");
        child.setParentId(100L);

        BlogArticle article = new BlogArticle();
        article.setId(10L);
        article.setCommentCount(4);

        when(sysCommentService.getById(100L)).thenReturn(comment);
        when(sysCommentService.getById(50L)).thenReturn(parent);
        when(sysCommentService.lambdaQuery()).thenReturn(commentTreeQuery);
        when(commentTreeQuery.eq(anySFunction(), any())).thenReturn(commentTreeQuery);
        when(commentTreeQuery.list()).thenReturn(List.of(parent, comment, child));
        when(sysCommentService.removeByIds(List.of(100L, 101L))).thenReturn(true);
        when(sysInteractionService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(blogArticleService.getById(10L)).thenReturn(article);
        when(blogArticleService.updateById(article)).thenReturn(true);
        when(sysCommentService.updateById(parent)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.deleteComment(100L);
        }

        assertEquals(Integer.valueOf(2), article.getCommentCount());
        assertEquals(Integer.valueOf(1), parent.getReplyCount());
        verify(sysCommentService).removeByIds(List.of(100L, 101L));
        verify(sysInteractionService).remove(any(LambdaQueryWrapper.class));
        verify(blogArticleService).updateById(article);
        verify(sysCommentService).updateById(parent);
    }

    @Test
    void likeCommentShouldCreateInteractionAndIncreaseLikeCount() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        SysInteraction interaction = new SysInteraction();
        interaction.setUserId(7L);
        interaction.setTargetId(100L);

        when(sysCommentService.getById(100L)).thenReturn(comment);
        when(sysInteractionService.lambdaQuery()).thenReturn(interactionExistsQuery);
        when(interactionExistsQuery.eq(anySFunction(), any())).thenReturn(interactionExistsQuery);
        when(interactionExistsQuery.exists()).thenReturn(false);
        when(contentModelMapper.toInteraction(7L, 100L, "comment", "like")).thenReturn(interaction);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.likeComment(100L);
        }

        assertEquals(Integer.valueOf(3), comment.getLikeCount());
        verify(sysInteractionService).save(interaction);
        verify(sysCommentService).updateById(comment);
    }

    @Test
    void likeCommentShouldReturnWhenAlreadyLiked() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        when(sysCommentService.getById(100L)).thenReturn(comment);
        when(sysInteractionService.lambdaQuery()).thenReturn(interactionExistsQuery);
        when(interactionExistsQuery.eq(anySFunction(), any())).thenReturn(interactionExistsQuery);
        when(interactionExistsQuery.exists()).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.likeComment(100L);
        }

        assertEquals(Integer.valueOf(2), comment.getLikeCount());
        verify(sysInteractionService, never()).save(any(SysInteraction.class));
        verify(sysCommentService, never()).updateById(comment);
    }

    @Test
    void unlikeCommentShouldRemoveInteractionAndRollbackLikeCount() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        SysInteraction interaction = new SysInteraction();
        interaction.setId(200L);
        interaction.setTargetId(100L);

        when(sysCommentService.getById(100L)).thenReturn(comment);
        when(sysInteractionService.lambdaQuery()).thenReturn(interactionLookupQuery);
        when(interactionLookupQuery.eq(anySFunction(), any())).thenReturn(interactionLookupQuery);
        when(interactionLookupQuery.one()).thenReturn(interaction);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.unlikeComment(100L);
        }

        assertEquals(Integer.valueOf(1), comment.getLikeCount());
        verify(sysInteractionService).removeById(200L);
        verify(sysCommentService).updateById(comment);
    }

    @Test
    void unlikeCommentShouldReturnWhenInteractionMissing() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        when(sysCommentService.getById(100L)).thenReturn(comment);
        when(sysInteractionService.lambdaQuery()).thenReturn(interactionLookupQuery);
        when(interactionLookupQuery.eq(anySFunction(), any())).thenReturn(interactionLookupQuery);
        when(interactionLookupQuery.one()).thenReturn(null);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.unlikeComment(100L);
        }

        assertEquals(Integer.valueOf(2), comment.getLikeCount());
        verify(sysInteractionService, never()).removeById(any(Long.class));
        verify(sysCommentService, never()).updateById(comment);
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}

