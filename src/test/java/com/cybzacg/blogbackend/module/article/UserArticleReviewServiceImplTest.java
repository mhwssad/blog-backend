package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleReviewLog;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.article.ArticleReviewActionEnum;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.model.common.ArticleReviewLogVO;
import com.cybzacg.blogbackend.module.article.model.user.ArticleReviewSubmitRequest;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleReviewLogRepository;
import com.cybzacg.blogbackend.module.article.service.impl.UserArticleReviewServiceImpl;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * UserArticleReviewServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserArticleReviewServiceImplTest {

    @Mock
    private BlogArticleRepository blogArticleRepository;
    @Mock
    private BlogArticleReviewLogRepository blogArticleReviewLogRepository;
    @Mock
    private SysUserRepository sysUserRepository;

    private UserArticleReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserArticleReviewServiceImpl(
                blogArticleRepository,
                blogArticleReviewLogRepository,
                sysUserRepository
        );
    }

    @Test
    void submitReviewShouldSucceedForDraftArticle() {
        Long userId = 1L;
        Long articleId = 10L;
        BlogArticle article = buildArticle(articleId, userId, ArticleReviewStatusEnum.NOT_SUBMITTED.getValue());
        article.setScheduledPublishTime(null);

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        ArticleReviewSubmitRequest request = new ArticleReviewSubmitRequest();
        request.setReviewComment("首次提交审核");

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            service.submitReview(articleId, request);

            assertEquals(ArticleReviewStatusEnum.REVIEWING.getValue(), article.getReviewStatus());
            verify(blogArticleRepository).updateById(article);

            ArgumentCaptor<BlogArticleReviewLog> logCaptor = ArgumentCaptor.forClass(BlogArticleReviewLog.class);
            verify(blogArticleReviewLogRepository).save(logCaptor.capture());
            BlogArticleReviewLog savedLog = logCaptor.getValue();
            assertEquals(articleId, savedLog.getArticleId());
            assertEquals(ArticleReviewActionEnum.SUBMIT.getCode(), savedLog.getActionType());
            assertEquals(ArticleReviewStatusEnum.NOT_SUBMITTED.getValue(), savedLog.getFromReviewStatus());
            assertEquals(ArticleReviewStatusEnum.REVIEWING.getValue(), savedLog.getToReviewStatus());
            assertEquals(userId, savedLog.getOperatorUserId());
            assertEquals("首次提交审核", savedLog.getReviewComment());
        }
    }

    @Test
    void submitReviewShouldRejectDuplicateWhenReviewing() {
        Long userId = 1L;
        Long articleId = 10L;
        BlogArticle article = buildArticle(articleId, userId, ArticleReviewStatusEnum.REVIEWING.getValue());

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitReview(articleId, new ArticleReviewSubmitRequest()));
            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
            assertEquals("当前文章已在审核中，请勿重复提交", ex.getMessage());

            verify(blogArticleRepository, never()).updateById(any());
            verify(blogArticleReviewLogRepository, never()).save(any());
        }
    }

    @Test
    void submitReviewShouldRejectNonOwnedArticle() {
        Long currentUserId = 1L;
        Long articleOwnerId = 999L;
        Long articleId = 10L;
        BlogArticle article = buildArticle(articleId, articleOwnerId, ArticleReviewStatusEnum.NOT_SUBMITTED.getValue());

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(currentUserId)) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitReview(articleId, new ArticleReviewSubmitRequest()));
            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), ex.getCode());
            assertEquals("只能操作自己的文章审核", ex.getMessage());

            verify(blogArticleRepository, never()).updateById(any());
        }
    }

    @Test
    void submitReviewShouldUseResubmitActionForRejectedArticle() {
        Long userId = 1L;
        Long articleId = 10L;
        BlogArticle article = buildArticle(articleId, userId, ArticleReviewStatusEnum.REJECTED.getValue());
        article.setScheduledPublishTime(null);

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        ArticleReviewSubmitRequest request = new ArticleReviewSubmitRequest();
        request.setReviewComment("修改后重新提交");

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            service.submitReview(articleId, request);

            assertEquals(ArticleReviewStatusEnum.REVIEWING.getValue(), article.getReviewStatus());

            ArgumentCaptor<BlogArticleReviewLog> logCaptor = ArgumentCaptor.forClass(BlogArticleReviewLog.class);
            verify(blogArticleReviewLogRepository).save(logCaptor.capture());
            BlogArticleReviewLog savedLog = logCaptor.getValue();
            assertEquals(ArticleReviewActionEnum.RESUBMIT.getCode(), savedLog.getActionType());
            assertEquals(ArticleReviewStatusEnum.REJECTED.getValue(), savedLog.getFromReviewStatus());
            assertEquals(ArticleReviewStatusEnum.REVIEWING.getValue(), savedLog.getToReviewStatus());
        }
    }

    @Test
    void listReviewLogsShouldReturnCompleteLogChain() {
        Long userId = 1L;
        Long articleId = 10L;
        BlogArticle article = buildArticle(articleId, userId, ArticleReviewStatusEnum.REVIEWING.getValue());

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        BlogArticleReviewLog log1 = buildReviewLog(1L, articleId, "submit", 0, 1, userId, LocalDateTime.now());
        BlogArticleReviewLog log2 = buildReviewLog(2L, articleId, "reject", 1, 3, 99L, LocalDateTime.now());
        when(blogArticleReviewLogRepository.listByArticleId(articleId)).thenReturn(List.of(log1, log2));

        SysUser operator1 = buildSysUser(userId, "author", "作者");
        SysUser operator2 = buildSysUser(99L, "admin", "管理员");
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(operator1, operator2));

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            List<ArticleReviewLogVO> result = service.listReviewLogs(articleId);

            assertEquals(2, result.size());
            assertEquals("submit", result.get(0).getActionType());
            assertEquals("reject", result.get(1).getActionType());
            assertEquals("作者", result.get(0).getOperatorNickname());
            assertEquals("管理员", result.get(1).getOperatorNickname());
        }
    }

    @Test
    void submitReviewShouldRejectScheduledArticle() {
        Long userId = 1L;
        Long articleId = 10L;
        BlogArticle article = buildArticle(articleId, userId, ArticleReviewStatusEnum.NOT_SUBMITTED.getValue());
        // 定时发布时间在未来
        article.setScheduledPublishTime(LocalDateTime.now().plusDays(1));

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(userId)) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.submitReview(articleId, new ArticleReviewSubmitRequest()));
            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
            assertEquals("请先取消未来定时发布后再提交审核", ex.getMessage());

            verify(blogArticleRepository, never()).updateById(any());
        }
    }

    // ===================== helpers =====================

    private BlogArticle buildArticle(Long id, Long authorId, Integer reviewStatus) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setAuthorId(authorId);
        article.setReviewStatus(reviewStatus);
        article.setScheduledPublishTime(null);
        return article;
    }

    private BlogArticleReviewLog buildReviewLog(Long id, Long articleId, String actionType,
                                                 Integer fromStatus, Integer toStatus,
                                                 Long operatorUserId, LocalDateTime operatedAt) {
        BlogArticleReviewLog log = new BlogArticleReviewLog();
        log.setId(id);
        log.setArticleId(articleId);
        log.setActionType(actionType);
        log.setFromReviewStatus(fromStatus);
        log.setToReviewStatus(toStatus);
        log.setOperatorUserId(operatorUserId);
        log.setOperatedAt(operatedAt);
        return log;
    }

    private SysUser buildSysUser(Long id, String username, String nickname) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }
}
