package com.cybzacg.blogbackend.module.article;


import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleReviewLog;
import com.cybzacg.blogbackend.enums.article.ArticleReviewActionEnum;
import com.cybzacg.blogbackend.enums.article.ArticleReviewStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewDecisionRequest;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewRepairRequest;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleReviewLogRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.impl.ArticleReviewAdminServiceImpl;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysTagRelationRepository;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ArticleReviewAdminServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ArticleReviewAdminServiceImplTest {

    @Mock
    private BlogArticleRepository blogArticleRepository;
    @Mock
    private BlogArticleReviewLogRepository blogArticleReviewLogRepository;
    @Mock
    private BlogArticleCategoryRepository blogArticleCategoryRepository;
    @Mock
    private SysTagRelationRepository sysTagRelationRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private ArticleModelMapper articleModelMapper;
    @Mock
    private ArticleAccessControlService articleAccessControlService;

    private ArticleReviewAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticleReviewAdminServiceImpl(
                blogArticleRepository,
                blogArticleReviewLogRepository,
                blogArticleCategoryRepository,
                sysTagRelationRepository,
                sysUserRepository,
                articleModelMapper,
                articleAccessControlService
        );
    }

    @Test
    void approveReviewShouldSetApprovedStatus() {
        Long articleId = 10L;
        Long adminId = 99L;
        BlogArticle article = buildArticle(articleId, ArticleReviewStatusEnum.REVIEWING.getValue());

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(adminId)) {
            service.approveReview(articleId, new ArticleReviewDecisionRequest());

            assertEquals(ArticleReviewStatusEnum.APPROVED.getValue(), article.getReviewStatus());
            verify(blogArticleRepository).updateById(article);

            ArgumentCaptor<BlogArticleReviewLog> logCaptor = ArgumentCaptor.forClass(BlogArticleReviewLog.class);
            verify(blogArticleReviewLogRepository).save(logCaptor.capture());
            BlogArticleReviewLog savedLog = logCaptor.getValue();
            assertEquals(articleId, savedLog.getArticleId());
            assertEquals(ArticleReviewActionEnum.APPROVE.getCode(), savedLog.getActionType());
            assertEquals(ArticleReviewStatusEnum.REVIEWING.getValue(), savedLog.getFromReviewStatus());
            assertEquals(ArticleReviewStatusEnum.APPROVED.getValue(), savedLog.getToReviewStatus());
            assertEquals(adminId, savedLog.getOperatorUserId());
        }
    }

    @Test
    void rejectReviewShouldRequireComment() {
        Long articleId = 10L;

        // 无 comment
        ArticleReviewDecisionRequest emptyRequest = new ArticleReviewDecisionRequest();
        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> service.rejectReview(articleId, emptyRequest));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex1.getCode());
        assertEquals("审核拒绝必须填写原因", ex1.getMessage());

        // null request
        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> service.rejectReview(articleId, null));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex2.getCode());

        // 空白 comment
        ArticleReviewDecisionRequest blankRequest = new ArticleReviewDecisionRequest();
        blankRequest.setReviewComment("   ");
        BusinessException ex3 = assertThrows(BusinessException.class,
                () -> service.rejectReview(articleId, blankRequest));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex3.getCode());

        verify(blogArticleRepository, never()).updateById(any());
    }

    @Test
    void rejectReviewShouldSetRejectedStatus() {
        Long articleId = 10L;
        Long adminId = 99L;
        BlogArticle article = buildArticle(articleId, ArticleReviewStatusEnum.REVIEWING.getValue());

        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        ArticleReviewDecisionRequest request = new ArticleReviewDecisionRequest();
        request.setReviewComment("内容质量不达标");

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(adminId)) {
            service.rejectReview(articleId, request);

            assertEquals(ArticleReviewStatusEnum.REJECTED.getValue(), article.getReviewStatus());
            verify(blogArticleRepository).updateById(article);

            ArgumentCaptor<BlogArticleReviewLog> logCaptor = ArgumentCaptor.forClass(BlogArticleReviewLog.class);
            verify(blogArticleReviewLogRepository).save(logCaptor.capture());
            BlogArticleReviewLog savedLog = logCaptor.getValue();
            assertEquals(ArticleReviewActionEnum.REJECT.getCode(), savedLog.getActionType());
            assertEquals(ArticleReviewStatusEnum.REJECTED.getValue(), savedLog.getToReviewStatus());
            assertEquals("内容质量不达标", savedLog.getReviewComment());
        }
    }

    @Test
    void repairReviewStatusShouldValidateTargetStatus() {
        Long articleId = 10L;
        Long adminId = 99L;
        // 当前状态：REVIEWING
        BlogArticle article = buildArticle(articleId, ArticleReviewStatusEnum.REVIEWING.getValue());
        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        ArticleReviewRepairRequest request = new ArticleReviewRepairRequest();
        request.setTargetReviewStatus(ArticleReviewStatusEnum.APPROVED.getValue());
        request.setReviewComment("误审核，修正为通过");

        try (MockedStatic<SecurityUtils> securityUtils = SecurityTestUtils.mockUserId(adminId)) {
            service.repairReviewStatus(articleId, request);

            assertEquals(ArticleReviewStatusEnum.APPROVED.getValue(), article.getReviewStatus());
            verify(blogArticleRepository).updateById(article);

            ArgumentCaptor<BlogArticleReviewLog> logCaptor = ArgumentCaptor.forClass(BlogArticleReviewLog.class);
            verify(blogArticleReviewLogRepository).save(logCaptor.capture());
            BlogArticleReviewLog savedLog = logCaptor.getValue();
            assertEquals(ArticleReviewActionEnum.REPAIR.getCode(), savedLog.getActionType());
            assertEquals(ArticleReviewStatusEnum.REVIEWING.getValue(), savedLog.getFromReviewStatus());
            assertEquals(ArticleReviewStatusEnum.APPROVED.getValue(), savedLog.getToReviewStatus());
        }
    }

    @Test
    void repairReviewStatusShouldRejectWhenSameAsCurrent() {
        Long articleId = 10L;
        BlogArticle article = buildArticle(articleId, ArticleReviewStatusEnum.APPROVED.getValue());
        when(blogArticleRepository.getById(articleId)).thenReturn(article);

        ArticleReviewRepairRequest request = new ArticleReviewRepairRequest();
        request.setTargetReviewStatus(ArticleReviewStatusEnum.APPROVED.getValue());
        request.setReviewComment("已经是审核通过");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.repairReviewStatus(articleId, request));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
        assertEquals("目标审核状态与当前状态一致，无需修正", ex.getMessage());
        verify(blogArticleRepository, never()).updateById(any());
    }

    @Test
    void reviewArticleShouldRejectWhenNotInReviewingStatus() {
        Long articleId = 10L;

        // 当前状态：NOT_SUBMITTED
        BlogArticle draftArticle = buildArticle(articleId, ArticleReviewStatusEnum.NOT_SUBMITTED.getValue());
        when(blogArticleRepository.getById(articleId)).thenReturn(draftArticle);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approveReview(articleId, new ArticleReviewDecisionRequest()));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
        assertEquals("当前文章不在待审核状态", ex.getMessage());

        verify(blogArticleRepository, never()).updateById(any());
    }

    // ===================== helpers =====================

    private BlogArticle buildArticle(Long id, Integer reviewStatus) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setAuthorId(1L);
        article.setReviewStatus(reviewStatus);
        return article;
    }
}
