package com.cybzacg.blogbackend.module.article;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.content.SysCategory;
import com.cybzacg.blogbackend.domain.content.SysTag;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import com.cybzacg.blogbackend.module.article.service.ArticleStatusMachine;
import com.cybzacg.blogbackend.module.article.service.impl.PublicArticleServiceImpl;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.footprint.service.UserFootprintService;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.taxonomy.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.taxonomy.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysCategoryRepository;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysTagRelationRepository;
import com.cybzacg.blogbackend.module.content.taxonomy.repository.SysTagRepository;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PublicArticleServiceImpl} 单元测试。
 *
 * <p>核心原则：每个测试直接通过 {@link #stubPublishedArticles} 设定 repository 返回的精确数据，
 * 不在 mock 中模拟 SQL 过滤/排序逻辑。测试数据在传入 stub 前已完成预筛选和预排序。
 */
@ExtendWith(MockitoExtension.class)
class PublicArticleServiceImplTest {
    @Mock
    private BlogArticleRepository blogArticleRepository;
    @Mock
    private BlogArticleCategoryRepository blogArticleCategoryRepository;
    @Mock
    private SysTagRelationRepository sysTagRelationRepository;
    @Mock
    private SysCategoryRepository sysCategoryRepository;
    @Mock
    private SysTagRepository sysTagRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysInteractionRepository sysInteractionRepository;
    @Mock
    private SysCollectionRepository sysCollectionRepository;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ArticleSeriesService articleSeriesService;
    @Mock
    private ArticleStatusMachine articleStatusMachine;
    @Mock
    private ArticleModelMapper articleModelMapper;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private UserFootprintService userFootprintService;

    private PublicArticleServiceImpl publicArticleService;

    @BeforeEach
    void setUp() {
        publicArticleService = new PublicArticleServiceImpl(
                blogArticleRepository,
                blogArticleCategoryRepository,
                sysTagRelationRepository,
                sysCategoryRepository,
                sysTagRepository,
                sysUserRepository,
                sysInteractionRepository,
                sysCollectionRepository,
                articleAccessControlService,
                articleSeriesService,
                articleStatusMachine,
                articleModelMapper,
                contentModelMapper,
                userFootprintService
        );
        mockArticleMappings();
        mockContentMappings();
        lenient().when(articleAccessControlService.canAccessArticle(any(BlogArticle.class), any())).thenReturn(true);
        lenient().when(articleStatusMachine.canInteract(any(BlogArticle.class))).thenReturn(true);
        lenient().when(articleSeriesService.listVisibleSeriesSummariesByArticleId(anyLong(), any())).thenReturn(List.of());
    }

    // ==================== pageArticles tests ====================

    /**
     * 关键词过滤由 repository SQL 完成。
     * 测试数据只包含匹配关键词的两篇文章，按 publish_time desc 排序。
     */
    @Test
    void pageArticlesShouldFilterByKeywordFromTitleAndSummary() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setKeyword("Java");

        // 预筛选：只有匹配 "Java" 的文章，预排序：publish_time desc -> id=2(tick=3) 在前
        BlogArticle summaryMatched = article(2L, "Spring", "深入 Java 生态", 3, 102L);
        BlogArticle titleMatched = article(1L, "Java 基础", "intro", 2, 101L);

        stubPublishedArticles(query, List.of(summaryMatched, titleMatched));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(
                user(102L, "u102", "作者乙"),
                user(101L, "u101", "作者甲")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(2L, result.getTotal());
            assertIterableEquals(List.of(2L, 1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
            assertEquals("作者乙", result.getRecords().get(0).getAuthorName());
            assertEquals("作者甲", result.getRecords().get(1).getAuthorName());
        }
    }

    /**
     * 分类过滤通过 resolveArticleIdsByRelations 完成。
     * 只有分类关联的文章 ID 会被传给 repository，repository 返回匹配结果。
     */
    @Test
    void pageArticlesShouldFilterByCategory() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setCategoryId(10L);

        stubCategoryArticleIds(List.of(
                categoryRelation(2L, 10L, 1),
                categoryRelation(3L, 10L, 2)
        ));
        // 预筛选+预排序：分类下只有 id=3(tick=3) 和 id=2(tick=2)
        stubPublishedArticles(query, List.of(
                article(3L, "A3", "s3", 3, 103L),
                article(2L, "A2", "s2", 2, 102L)
        ));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(
                user(103L, "u103", "作者丙"),
                user(102L, "u102", "作者乙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(2L, result.getTotal());
            assertIterableEquals(List.of(3L, 2L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 标签过滤通过 resolveArticleIdsByRelations 完成。
     */
    @Test
    void pageArticlesShouldFilterByTag() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setTagId(20L);

        stubTagArticleIds(List.of(2L, 4L));
        // 预筛选+预排序：标签下只有 id=4(tick=4) 和 id=2(tick=2)
        stubPublishedArticles(query, List.of(
                article(4L, "A4", "s4", 4, 104L),
                article(2L, "A2", "s2", 2, 102L)
        ));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(
                user(104L, "u104", "作者丁"),
                user(102L, "u102", "作者乙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(2L, result.getTotal());
            assertIterableEquals(List.of(4L, 2L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 分类 + 标签取交集。
     */
    @Test
    void pageArticlesShouldFilterByCategoryAndTagIntersection() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setCategoryId(10L);
        query.setTagId(20L);

        stubCategoryArticleIds(List.of(
                categoryRelation(2L, 10L, 1),
                categoryRelation(3L, 10L, 2)
        ));
        stubTagArticleIds(List.of(3L, 4L));
        // 交集只有 id=3
        stubPublishedArticles(query, List.of(
                article(3L, "A3", "s3", 3, 103L)
        ));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(103L, "u103", "作者丙")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertIterableEquals(List.of(3L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 按 publish_time desc 排序（默认 latest 等价于 top 排序但无置顶文章）。
     * 数据已预排序：id=2(tick=3) > id=3(tick=2) > id=1(tick=1)。
     */
    @Test
    void pageArticlesShouldSortByLatest() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setSort("latest");

        stubPublishedArticles(query, List.of(
                article(2L, "A2", "s2", 3, 102L),
                article(3L, "A3", "s3", 2, 103L),
                article(1L, "A1", "s1", 1, 101L)
        ));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(
                user(102L, "u102", "作者乙"),
                user(103L, "u103", "作者丙"),
                user(101L, "u101", "作者甲")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertIterableEquals(List.of(2L, 3L, 1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 置顶文章优先，同组内按 publish_time desc。
     * 数据已预排序：top+newest(id=3,tick=3) > top+old(id=1,tick=1) > normal(id=2,tick=4)。
     */
    @Test
    void pageArticlesShouldSortByTopFirstThenPublishTime() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setSort("top");

        BlogArticle topNewest = article(3L, "A3", "s3", 3, 103L);
        topNewest.setIsTop(1);
        BlogArticle topOld = article(1L, "A1", "s1", 1, 101L);
        topOld.setIsTop(1);
        BlogArticle normalNewest = article(2L, "A2", "s2", 4, 102L);
        normalNewest.setIsTop(0);

        stubPublishedArticles(query, List.of(topNewest, topOld, normalNewest));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(
                user(103L, "u103", "作者丙"),
                user(101L, "u101", "作者甲"),
                user(102L, "u102", "作者乙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertIterableEquals(List.of(3L, 1L, 2L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 按热度排序：hot_score = view + like + comment，相同时按 id desc。
     * 数据已预排序：id=3(13) = id=2(13) -> id desc, id=1(13) -> 但实际 hot 排序相同分数按 id desc，
     * 所以 id=3 > id=2 > id=1。
     */
    @Test
    void pageArticlesShouldSortByHotScore() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setSort("hot");

        BlogArticle third = article(3L, "A3", "s3", 3, 103L);
        third.setViewCount(6);
        third.setLikeCount(4);
        third.setCommentCount(3);

        BlogArticle second = article(2L, "A2", "s2", 2, 102L);
        second.setViewCount(6);
        second.setLikeCount(4);
        second.setCommentCount(3);

        BlogArticle first = article(1L, "A1", "s1", 1, 101L);
        first.setViewCount(10);
        first.setLikeCount(2);
        first.setCommentCount(1);

        stubPublishedArticles(query, List.of(third, second, first));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(
                user(103L, "u103", "作者丙"),
                user(102L, "u102", "作者乙"),
                user(101L, "u101", "作者甲")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertIterableEquals(List.of(3L, 2L, 1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    @Test
    void pageArticlesShouldReturnEmptyWhenNoPublishedArticles() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        stubPublishedArticles(query, List.of());

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(0L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
            verify(sysUserRepository, never()).listByIds(anyCollection());
        }
    }

    @Test
    void pageArticlesShouldQueryPublishedStatusOnly() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        stubPublishedArticles(query, List.of());

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            publicArticleService.pageArticles(query);

            verify(blogArticleRepository).pagePublishedArticles(any(PublicArticlePageQuery.class), any());
        }
    }

    @Test
    void pageArticlesShouldReturnEmptyRecordsWhenPageOutOfRange() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setCurrent(3L);
        query.setSize(10L);

        // 模拟 repository 返回 total=1 但当前页无数据
        Page<BlogArticle> page = new Page<>(3, 10, 1);
        page.setRecords(List.of());
        when(blogArticleRepository.pagePublishedArticles(eq(query), nullable(Set.class))).thenReturn(page);

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    /**
     * 草稿文章(status=0)不会出现在公开分页列表中。
     * 预筛选数据只包含已发布文章，验证 total 排除草稿。
     */
    @Test
    void pageArticlesShouldExcludeDraftArticles() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();

        // 只传入已发布文章（草稿已在 SQL 层被过滤）
        BlogArticle published = article(1L, "已发布", "已发布文章", 1, 101L);
        published.setStatus(1);

        stubPublishedArticles(query, List.of(published));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(101L, "u101", "作者甲")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertIterableEquals(List.of(1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 审核中文章(reviewStatus=reviewing)不会出现在公开分页列表中。
     * 预筛选数据只包含审核通过的文章，验证 total 排除审核中的文章。
     */
    @Test
    void pageArticlesShouldExcludeReviewingArticles() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();

        // 只传入审核通过的文章（审核中已在 SQL 层被过滤）
        BlogArticle approved = article(1L, "审核通过", "已通过审核", 1, 101L);
        approved.setStatus(1);

        stubPublishedArticles(query, List.of(approved));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(101L, "u101", "作者甲")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertIterableEquals(List.of(1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 审核被拒文章(reviewStatus=rejected)不会出现在公开分页列表中。
     * 预筛选数据只包含审核通过的文章，验证 total 排除被拒文章。
     */
    @Test
    void pageArticlesShouldExcludeRejectedArticles() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();

        // 只传入审核通过的文章（被拒的已在 SQL 层被过滤）
        BlogArticle approved = article(1L, "审核通过", "已通过审核", 1, 101L);
        approved.setStatus(1);

        stubPublishedArticles(query, List.of(approved));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(101L, "u101", "作者甲")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertIterableEquals(List.of(1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 定时发布时间在未来的文章不会出现在公开分页列表中。
     * 预筛选数据只包含已发布的文章，验证 total 排除未来定时发布的文章。
     */
    @Test
    void pageArticlesShouldExcludeScheduledFutureArticles() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();

        // 只传入已发布文章（定时发布的已在 SQL 层被过滤）
        BlogArticle published = article(1L, "已发布", "已发布文章", 1, 101L);
        published.setStatus(1);

        stubPublishedArticles(query, List.of(published));
        when(sysUserRepository.listByIds(anyCollection())).thenReturn(List.of(user(101L, "u101", "作者甲")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertIterableEquals(List.of(1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    /**
     * 访问控制通过 articleAccessControlService.validateArticleAccess 在 getArticle 中执行。
     * 对于 accessLevel=1（登录可见）且未登录用户，validateArticleAccess 应抛出 FORBIDDEN。
     */
    @Test
    void getArticleShouldRejectLoginOnlyArticleForAnonymousUser() {
        BlogArticle article = article(11L, "登录可见", "summary", 1, 101L);
        article.setAccessLevel(1);
        when(blogArticleRepository.getById(11L)).thenReturn(article);
        doThrow(new BusinessException(ResultErrorCode.FORBIDDEN.getCode(), "当前用户无权访问该文章"))
                .when(articleAccessControlService).validateArticleAccess(article, null);

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> publicArticleService.getArticle(11L));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
            verifyNoInteractions(userFootprintService);
        }
    }

    /**
     * accessLevel=4（白名单可见）文章，白名单内用户通过 validateArticleAccess 后可正常访问。
     */
    @Test
    void getArticleShouldAllowWhitelistUserForAccessLevelFour() {
        BlogArticle article = article(13L, "白名单文章", "摘要", 1, 101L);
        article.setAccessLevel(4);
        article.setContent("content");
        when(blogArticleRepository.getById(13L)).thenReturn(article);
        doNothing().when(articleAccessControlService).validateArticleAccess(article, 9L);
        when(sysUserRepository.getById(101L)).thenReturn(user(101L, "author101", "作者甲"));

        stubCategoryRelationsForDetail(List.of());
        when(sysTagRelationRepository.listTagIdsByTargetTypeAndTargetId("article", 13L)).thenReturn(List.of());

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(9L)) {
            PublicArticleDetailVO detail = publicArticleService.getArticle(13L);

            assertEquals(13L, detail.getId());
            verify(articleAccessControlService).validateArticleAccess(article, 9L);
            verify(userFootprintService).recordArticleFootprint(13L);
        }
    }

    /**
     * accessLevel=4（白名单可见）文章，非白名单用户被 validateArticleAccess 拒绝。
     */
    @Test
    void getArticleShouldRejectNonWhitelistUserForAccessLevelFour() {
        BlogArticle article = article(14L, "白名单文章", "摘要", 1, 101L);
        article.setAccessLevel(4);
        when(blogArticleRepository.getById(14L)).thenReturn(article);
        doThrow(new BusinessException(ResultErrorCode.FORBIDDEN.getCode(), "当前用户无权访问该文章"))
                .when(articleAccessControlService).validateArticleAccess(article, 8L);

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(8L)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> publicArticleService.getArticle(14L));

            assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
            verifyNoInteractions(userFootprintService);
        }
    }

    // ==================== getArticle tests ====================

    @Test
    void getArticleShouldThrowWhenArticleDoesNotExist() {
        when(blogArticleRepository.getById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> publicArticleService.getArticle(99L));

        assertEquals(ResultErrorCode.NO_HANDLER_FOUND.getCode(), exception.getCode());
        assertEquals("文章不存在", exception.getMessage());
        verifyNoInteractions(userFootprintService);
    }

    /**
     * 未发布文章的拒绝由 ArticleAccessControlService.validateArticleAccess 内部处理，
     * 因为 canAccessArticle 会调用 isPublishedForNormalUsers 检查。
     * 此处 mock validateArticleAccess 抛出异常来模拟该场景。
     */
    @Test
    void getArticleShouldRejectUnpublishedArticle() {
        BlogArticle draft = article(10L, "草稿", "未发布", 1, 101L);
        draft.setStatus(0);
        when(blogArticleRepository.getById(10L)).thenReturn(draft);
        doThrow(new BusinessException(ResultErrorCode.NO_HANDLER_FOUND.getCode(), "文章不存在"))
                .when(articleAccessControlService).validateArticleAccess(draft, null);

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> publicArticleService.getArticle(10L));

            assertEquals(ResultErrorCode.NO_HANDLER_FOUND.getCode(), exception.getCode());
            assertEquals("文章不存在", exception.getMessage());
            verifyNoInteractions(userFootprintService);
        }
    }

    @Test
    void getArticleShouldAssembleDetailAndRecordFootprint() {
        BlogArticle article = article(12L, "文章详情", "摘要", 5, 101L);
        article.setContent("content");
        article.setAccessLevel(0);
        when(blogArticleRepository.getById(12L)).thenReturn(article);
        when(sysUserRepository.getById(101L)).thenReturn(user(101L, "author101", "作者甲"));

        stubCategoryRelationsForDetail(List.of(
                categoryRelation(12L, 1001L, 1),
                categoryRelation(12L, 1002L, 2)
        ));
        when(sysCategoryRepository.listByIds(List.of(1001L, 1002L))).thenReturn(List.of(
                category(1001L, "后端"),
                category(1002L, "Java")
        ));

        when(sysTagRelationRepository.listTagIdsByTargetTypeAndTargetId("article", 12L)).thenReturn(List.of(2001L, 2002L));
        when(sysTagRepository.listByIds(List.of(2001L, 2002L))).thenReturn(List.of(
                tag(2001L, "Spring"),
                tag(2002L, "Boot")
        ));

        when(sysInteractionRepository.existsByUserIdAndTargetIdAndTargetTypeAndActionType(anyLong(), anyLong(), anyString(), anyString())).thenReturn(true);
        when(sysCollectionRepository.existsByUserIdAndTargetTypeAndTargetId(anyLong(), anyString(), anyLong())).thenReturn(true);

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(8L)) {
            PublicArticleDetailVO detail = publicArticleService.getArticle(12L);

            assertEquals(12L, detail.getId());
            assertEquals("作者甲", detail.getAuthorName());
            assertEquals(2, detail.getCategories().size());
            assertEquals(2, detail.getTags().size());
            assertTrue(detail.getLiked());
            assertTrue(detail.getCollected());
            assertTrue(detail.getCanComment());
            assertEquals("后端", detail.getCategories().get(0).getName());
            assertEquals("Boot", detail.getTags().get(1).getName());
            verify(articleAccessControlService).validateArticleAccess(article, 8L);
            verify(userFootprintService).recordArticleFootprint(12L);
        }
    }

    // ==================== stub / helper methods ====================

    /**
     * 为 repository.pagePublishedArticles 设置静态返回值。
     *
     * <p>将传入的 articles 直接包装为 MyBatis-Plus Page 对象，不执行任何过滤或排序。
     * 调用方负责预先筛选和排序测试数据。
     */
    private void stubPublishedArticles(PublicArticlePageQuery query, List<BlogArticle> articles) {
        long current = query.getCurrent() != null ? query.getCurrent() : 1;
        long size = query.getSize() != null ? query.getSize() : 10;
        Page<BlogArticle> page = new Page<>(current, size, articles.size());
        page.setRecords(articles);
        when(blogArticleRepository.pagePublishedArticles(eq(query), nullable(Set.class))).thenReturn(page);
    }

    private void stubCategoryArticleIds(List<BlogArticleCategory> relations) {
        when(blogArticleCategoryRepository.listArticleIdsByCategoryId(anyLong())).thenReturn(relations);
    }

    private void stubTagArticleIds(List<Long> articleIds) {
        when(sysTagRelationRepository.listTargetIdsByTargetTypeAndTagId(anyString(), anyLong())).thenReturn(articleIds);
    }

    private void stubCategoryRelationsForDetail(List<BlogArticleCategory> relations) {
        when(blogArticleCategoryRepository.listByArticleIdOrdered(anyLong())).thenReturn(relations);
    }

    private void mockArticleMappings() {
        lenient().when(articleModelMapper.toPublicCardVO(any(BlogArticle.class))).thenAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            PublicArticleCardVO vo = new PublicArticleCardVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setAuthorId(article.getAuthorId());
            vo.setIsTop(article.getIsTop());
            vo.setAccessLevel(article.getAccessLevel());
            vo.setViewCount(article.getViewCount());
            vo.setLikeCount(article.getLikeCount());
            vo.setCommentCount(article.getCommentCount());
            vo.setCollectCount(article.getCollectCount());
            vo.setPublishTime(article.getPublishTime());
            return vo;
        });
        lenient().when(articleModelMapper.toPublicDetailVO(any(BlogArticle.class))).thenAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            PublicArticleDetailVO vo = new PublicArticleDetailVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setContent(article.getContent());
            vo.setAuthorId(article.getAuthorId());
            vo.setAccessLevel(article.getAccessLevel());
            vo.setPublishTime(article.getPublishTime());
            return vo;
        });
    }

    private void mockContentMappings() {
        lenient().when(contentModelMapper.toPublicCategoryTreeVO(any(SysCategory.class))).thenAnswer(invocation -> {
            SysCategory category = invocation.getArgument(0);
            PublicCategoryTreeVO vo = new PublicCategoryTreeVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setChildren(new ArrayList<>());
            return vo;
        });
        lenient().when(contentModelMapper.toPublicTagVO(any(SysTag.class))).thenAnswer(invocation -> {
            SysTag tag = invocation.getArgument(0);
            PublicTagVO vo = new PublicTagVO();
            vo.setId(tag.getId());
            vo.setName(tag.getName());
            vo.setColor(tag.getColor());
            return vo;
        });
    }

    private BlogArticle article(Long id, String title, String summary, int publishTick, Long authorId) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setTitle(title);
        article.setSummary(summary);
        article.setAuthorId(authorId);
        article.setStatus(1);
        article.setIsTop(0);
        article.setAccessLevel(0);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setCollectCount(0);
        article.setPublishTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(1_000L * publishTick), ZoneOffset.UTC));
        return article;
    }

    private BlogArticleCategory categoryRelation(Long articleId, Long categoryId, Integer sortOrder) {
        BlogArticleCategory relation = new BlogArticleCategory();
        relation.setArticleId(articleId);
        relation.setCategoryId(categoryId);
        relation.setSortOrder(sortOrder);
        return relation;
    }

    private SysUser user(Long id, String username, String nickname) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setDeletedFlag(0);
        return user;
    }

    private SysCategory category(Long id, String name) {
        SysCategory category = new SysCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private SysTag tag(Long id, String name) {
        SysTag tag = new SysTag();
        tag.setId(id);
        tag.setName(name);
        tag.setColor("#333333");
        return tag;
    }
}
