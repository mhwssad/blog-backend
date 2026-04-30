package com.cybzacg.blogbackend.module.content.comment;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.comment.model.user.CommentSaveRequest;
import com.cybzacg.blogbackend.module.content.comment.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.comment.service.impl.UserCommentServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommentServiceImplTest {
    @Mock
    private SysCommentRepository sysCommentRepository;
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private ArticleContentFacadeService articleContentFacadeService;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    private UserCommentServiceImpl userCommentService;

    @BeforeEach
    void setUp() {
        userCommentService = new UserCommentServiceImpl(
                sysCommentRepository,
                sysInteractionRepository,
                articleContentFacadeService,
                contentModelMapper,
                eventPublisher,
                notificationDeliveryService
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
        article.setAuthorId(99L);

        SysComment parent = new SysComment();
        parent.setId(100L);
        parent.setTargetId(10L);
        parent.setTargetType("article");
        parent.setRootId(0L);
        parent.setReplyCount(1);

        SysComment comment = new SysComment();

        when(articleContentFacadeService.requireInteractableArticle(10L, 7L, "评论")).thenReturn(article);
        when(contentModelMapper.toComment(request)).thenReturn(comment);
        when(sysCommentRepository.getById(100L)).thenReturn(parent);
        when(sysCommentRepository.save(comment)).thenAnswer(invocation -> {
            comment.setId(101L);
            return true;
        });
        when(sysCommentRepository.updateById(parent)).thenReturn(true);

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
        verify(sysCommentRepository).save(comment);
        verify(sysCommentRepository).updateById(parent);
        verify(articleContentFacadeService).adjustCommentCount(10L, 1);
    }

    @Test
    void createCommentShouldRejectWhenArticleAccessDenied() {
        CommentSaveRequest request = new CommentSaveRequest();
        request.setTargetType("article");
        request.setTargetId(10L);
        request.setContent("blocked");

        when(articleContentFacadeService.requireInteractableArticle(10L, 7L, "评论"))
                .thenThrow(new BusinessException(ResultErrorCode.FORBIDDEN));

        BusinessException exception;
        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            exception = assertThrows(BusinessException.class, () -> userCommentService.createComment(request));
        }

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        verify(sysCommentRepository, never()).save(any(SysComment.class));
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
        comment.setRootId(50L);

        SysComment child = new SysComment();
        child.setId(101L);
        child.setUserId(8L);
        child.setTargetId(10L);
        child.setTargetType("article");
        child.setParentId(100L);

        when(sysCommentRepository.getById(100L)).thenReturn(comment);
        when(sysCommentRepository.getById(50L)).thenReturn(parent);
        when(sysCommentRepository.findByTargetTypeAndTargetId("article", 10L)).thenReturn(List.of(parent, comment, child));
        when(sysCommentRepository.removeByIds(List.of(100L, 101L))).thenReturn(true);
        when(sysInteractionRepository.removeByTargetTypeAndTargetIds("comment", List.of(100L, 101L))).thenReturn(true);
        when(sysCommentRepository.updateById(parent)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.deleteComment(100L);
        }

        assertEquals(Integer.valueOf(1), parent.getReplyCount());
        verify(sysCommentRepository).removeByIds(List.of(100L, 101L));
        verify(sysInteractionRepository).removeByTargetTypeAndTargetIds("comment", List.of(100L, 101L));
        verify(articleContentFacadeService).adjustCommentCount(10L, -2);
        verify(sysCommentRepository).updateById(parent);
    }

    @Test
    void likeCommentShouldCreateInteractionAndIncreaseLikeCount() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        SysInteraction interaction = new SysInteraction();
        interaction.setUserId(7L);
        interaction.setTargetId(100L);

        when(sysCommentRepository.getById(100L)).thenReturn(comment);
        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 100L, "comment", "like")).thenReturn(false);
        when(contentModelMapper.toInteraction(7L, 100L, "comment", "like")).thenReturn(interaction);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.likeComment(100L);
        }

        assertEquals(Integer.valueOf(3), comment.getLikeCount());
        verify(sysInteractionRepository).save(interaction);
        verify(sysCommentRepository).updateById(comment);
    }

    @Test
    void likeCommentShouldReturnWhenAlreadyLiked() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        when(sysCommentRepository.getById(100L)).thenReturn(comment);
        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 100L, "comment", "like")).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.likeComment(100L);
        }

        assertEquals(Integer.valueOf(2), comment.getLikeCount());
        verify(sysInteractionRepository, never()).save(any(SysInteraction.class));
        verify(sysCommentRepository, never()).updateById(comment);
    }

    @Test
    void unlikeCommentShouldRemoveInteractionAndRollbackLikeCount() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        SysInteraction interaction = new SysInteraction();
        interaction.setId(200L);
        interaction.setTargetId(100L);

        when(sysCommentRepository.getById(100L)).thenReturn(comment);
        when(sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 100L, "comment", "like")).thenReturn(interaction);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.unlikeComment(100L);
        }

        assertEquals(Integer.valueOf(1), comment.getLikeCount());
        verify(sysInteractionRepository).removeById(200L);
        verify(sysCommentRepository).updateById(comment);
    }

    @Test
    void unlikeCommentShouldReturnWhenInteractionMissing() {
        SysComment comment = new SysComment();
        comment.setId(100L);
        comment.setLikeCount(2);

        when(sysCommentRepository.getById(100L)).thenReturn(comment);
        when(sysInteractionRepository.findOneByUserIdAndTargetIdAndTargetTypeAndActionType(7L, 100L, "comment", "like")).thenReturn(null);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCommentService.unlikeComment(100L);
        }

        assertEquals(Integer.valueOf(2), comment.getLikeCount());
        verify(sysInteractionRepository, never()).removeById(any(Long.class));
        verify(sysCommentRepository, never()).updateById(comment);
    }
}