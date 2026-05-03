package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.domain.article.BlogArticleSeries;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.convert.ArticleSeriesModelConvert;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesArticleRequest;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSaveRequest;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesVO;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleSeriesItemRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleSeriesRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesItemService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.article.service.impl.ArticleSeriesServiceImpl;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link ArticleSeriesServiceImpl} 单元测试。
 *
 * <p>测试系列的创建、更新、删除权限控制，以及列表查询排序等核心逻辑。
 */
@ExtendWith(MockitoExtension.class)
class ArticleSeriesServiceImplTest {
    @Mock
    private BlogArticleSeriesRepository blogArticleSeriesRepository;
    @Mock
    private BlogArticleSeriesItemRepository blogArticleSeriesItemRepository;
    @Mock
    private BlogArticleRepository blogArticleRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private AuthorPermissionService authorPermissionService;
    @Mock
    private ArticleStatusMachine articleStatusMachine;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ArticleSeriesModelConvert articleSeriesModelConvert;
    @Mock
    private ArticleSeriesItemService articleSeriesItemService;

    private ArticleSeriesServiceImpl articleSeriesService;

    @BeforeEach
    void setUp() {
        articleSeriesService = new ArticleSeriesServiceImpl(
                blogArticleSeriesRepository,
                blogArticleSeriesItemRepository,
                blogArticleRepository,
                sysUserRepository,
                authorPermissionService,
                articleStatusMachine,
                articleAccessControlService,
                articleSeriesModelConvert,
                articleSeriesItemService
        );
    }

    /**
     * 作者用户创建系列，验证 repo.save 被调用且返回详情。
     */
    @Test
    void createSeriesShouldSucceedForAuthorUser() {
        ArticleSeriesSaveRequest request = new ArticleSeriesSaveRequest();
        request.setTitle("测试系列");
        request.setStatus(1);
        request.setVisibilityScope(0);

        BlogArticleSeries mappedSeries = new BlogArticleSeries();
        mappedSeries.setTitle("测试系列");
        mappedSeries.setStatus(1);
        mappedSeries.setVisibilityScope(0);
        when(articleSeriesModelConvert.toSeries(request)).thenReturn(mappedSeries);
        when(articleStatusMachine.normalizeVisibilityScope(any())).thenReturn(0);
        lenient().when(articleSeriesModelConvert.toUserSeriesDetailVO(any(BlogArticleSeries.class)))
                .thenReturn(new UserArticleSeriesDetailVO());
        lenient().when(blogArticleSeriesItemRepository.listBySeriesIdOrdered(anyLong()))
                .thenReturn(List.of());

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(authorPermissionService.hasAuthorRole(1L)).thenReturn(true);

            UserArticleSeriesDetailVO result = articleSeriesService.createSeries(request);

            assertNotNull(result);
            verify(blogArticleSeriesRepository).save(any(BlogArticleSeries.class));
        }
    }

    /**
     * 非作者用户创建系列，应抛出 FORBIDDEN。
     */
    @Test
    void createSeriesShouldRejectForNonAuthorUser() {
        ArticleSeriesSaveRequest request = new ArticleSeriesSaveRequest();
        request.setTitle("测试系列");

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(2L)) {
            when(authorPermissionService.hasAuthorRole(2L)).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleSeriesService.createSeries(request));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
            verify(blogArticleSeriesRepository, never()).save(any());
        }
    }

    /**
     * 添加已存在于系列中的文章，应抛出 ILLEGAL_ARGUMENT。
     * ArticleSeriesServiceImpl.addArticle 委托给 ArticleSeriesItemService，
     * 但重复检测逻辑在 ArticleSeriesItemServiceImpl 中，
     * 此处通过 mock articleSeriesItemService 模拟该行为。
     */
    @Test
    void addArticleShouldRejectDuplicateArticle() {
        Long seriesId = 10L;
        ArticleSeriesArticleRequest request = new ArticleSeriesArticleRequest();
        request.setArticleId(100L);

        // ArticleSeriesServiceImpl.addArticle 直接委托给 articleSeriesItemService
        // 通过 mock 让 itemService 抛出重复异常
        doThrow(new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "该文章已在当前系列中"))
                .when(articleSeriesItemService).addArticle(seriesId, request);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> articleSeriesService.addArticle(seriesId, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("该文章已在当前系列中", exception.getMessage());
    }

    /**
     * 非系列所有者尝试更新系列，应抛出 FORBIDDEN。
     */
    @Test
    void updateSeriesShouldRejectUnauthorizedUser() {
        Long seriesId = 10L;
        ArticleSeriesSaveRequest request = new ArticleSeriesSaveRequest();
        request.setTitle("修改标题");
        request.setStatus(1);
        request.setVisibilityScope(0);

        BlogArticleSeries series = new BlogArticleSeries();
        series.setId(seriesId);
        series.setOwnerUserId(1L); // 所有者是用户 1
        when(blogArticleSeriesRepository.getById(seriesId)).thenReturn(series);
        when(articleStatusMachine.normalizeVisibilityScope(any())).thenReturn(0);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(2L)) {
            when(authorPermissionService.hasAuthorRole(2L)).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleSeriesService.updateSeries(seriesId, request));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
            verify(blogArticleSeriesRepository, never()).updateById(any());
        }
    }

    /**
     * listMySeries 按 sortOrder 升序再按 id 升序返回。
     * 预筛选数据由 repo.listByOwnerUserId 返回，排序由 repo SQL 完成。
     */
    @Test
    void listMySeriesShouldReturnCorrectSortOrder() {
        BlogArticleSeries series1 = new BlogArticleSeries();
        series1.setId(1L);
        series1.setTitle("系列B");
        series1.setSortOrder(2);

        BlogArticleSeries series2 = new BlogArticleSeries();
        series2.setId(2L);
        series2.setTitle("系列A");
        series2.setSortOrder(1);

        BlogArticleSeries series3 = new BlogArticleSeries();
        series3.setId(3L);
        series3.setTitle("系列C");
        series3.setSortOrder(2);

        // 预排序：sortOrder=1(id=2) -> sortOrder=2(id=1) -> sortOrder=2(id=3)
        when(blogArticleSeriesRepository.listByOwnerUserId(1L))
                .thenReturn(List.of(series2, series1, series3));

        UserArticleSeriesVO vo2 = new UserArticleSeriesVO();
        vo2.setId(2L);
        vo2.setTitle("系列A");
        UserArticleSeriesVO vo1 = new UserArticleSeriesVO();
        vo1.setId(1L);
        vo1.setTitle("系列B");
        UserArticleSeriesVO vo3 = new UserArticleSeriesVO();
        vo3.setId(3L);
        vo3.setTitle("系列C");

        when(articleSeriesModelConvert.toUserSeriesVO(series2)).thenReturn(vo2);
        when(articleSeriesModelConvert.toUserSeriesVO(series1)).thenReturn(vo1);
        when(articleSeriesModelConvert.toUserSeriesVO(series3)).thenReturn(vo3);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(authorPermissionService.hasAuthorRole(1L)).thenReturn(true);

            List<UserArticleSeriesVO> result = articleSeriesService.listMySeries();

            assertEquals(3, result.size());
            // 按 sortOrder 升序再按 id 升序：sortOrder=1(id=2) -> sortOrder=2(id=1) -> sortOrder=2(id=3)
            assertIterableEquals(List.of(2L, 1L, 3L), result.stream().map(UserArticleSeriesVO::getId).toList());
        }
    }

    /**
     * 删除系列时只删除系列及关联关系，不删除系列内的文章。
     */
    @Test
    void deleteSeriesShouldNotDeleteArticles() {
        Long seriesId = 10L;

        BlogArticleSeries series = new BlogArticleSeries();
        series.setId(seriesId);
        series.setOwnerUserId(1L);
        when(blogArticleSeriesRepository.getById(seriesId)).thenReturn(series);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(authorPermissionService.hasAuthorRole(1L)).thenReturn(true);

            articleSeriesService.deleteSeries(seriesId);

            verify(blogArticleSeriesItemRepository).removeBySeriesId(seriesId);
            verify(blogArticleSeriesRepository).removeById(seriesId);
            // 确认文章仓库没有被调用删除
            verify(blogArticleRepository, never()).removeById(anyLong());
            verify(blogArticleRepository, never()).removeBatchByIds(anyCollection());
        }
    }
}
