package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminCrudService;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminModerationService;
import com.cybzacg.blogbackend.module.article.service.impl.ArticleAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleAdminServiceImplTest {
    @Mock
    private ArticleAdminCrudService articleAdminCrudService;
    @Mock
    private ArticleAdminModerationService articleAdminModerationService;

    private ArticleAdminServiceImpl articleAdminService;

    @BeforeEach
    void setUp() {
        articleAdminService = new ArticleAdminServiceImpl(
                articleAdminCrudService,
                articleAdminModerationService
        );
    }

    @Test
    void deleteArticleShouldDelegateToCrudService() {
        articleAdminService.deleteArticle(1L);
        verify(articleAdminCrudService).deleteArticle(1L);
    }

    @Test
    void updateStatusShouldDelegateToCrudService() {
        articleAdminService.updateStatus(1L, 1);
        verify(articleAdminCrudService).updateStatus(1L, 1);
    }

    @Test
    void createArticleShouldDelegateToCrudService() {
        ArticleSaveRequest request = new ArticleSaveRequest();
        request.setTitle("测试");
        ArticleDetailVO expected = new ArticleDetailVO();
        expected.setId(100L);
        when(articleAdminCrudService.createArticle(request)).thenReturn(expected);

        ArticleDetailVO result = articleAdminService.createArticle(request);

        assertEquals(100L, result.getId());
    }

    @Test
    void updateArticleShouldDelegateToCrudService() {
        ArticleSaveRequest request = new ArticleSaveRequest();
        ArticleDetailVO expected = new ArticleDetailVO();
        expected.setId(200L);
        when(articleAdminCrudService.updateArticle(200L, request)).thenReturn(expected);

        ArticleDetailVO result = articleAdminService.updateArticle(200L, request);

        assertEquals(200L, result.getId());
    }

    @Test
    void pageArticlesShouldDelegateToCrudService() {
        articleAdminService.pageArticles(null);
        verify(articleAdminCrudService).pageArticles(null);
    }

    @Test
    void getArticleShouldDelegateToCrudService() {
        articleAdminService.getArticle(1L);
        verify(articleAdminCrudService).getArticle(1L);
    }

    @Test
    void toggleTopShouldDelegateToModerationService() {
        articleAdminService.toggleTop(1L, true, 1L, "127.0.0.1", "test-agent");
        verify(articleAdminModerationService).toggleTop(1L, true, 1L, "127.0.0.1", "test-agent");
    }

    @Test
    void toggleRecommendShouldDelegateToModerationService() {
        articleAdminService.toggleRecommend(1L, true, 1L, "127.0.0.1", "test-agent");
        verify(articleAdminModerationService).toggleRecommend(1L, true, 1L, "127.0.0.1", "test-agent");
    }

    @Test
    void assignAccessShouldDelegateToModerationService() {
        articleAdminService.assignAccess(1L, List.of());
        verify(articleAdminModerationService).assignAccess(1L, List.of());
    }
}
