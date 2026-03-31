package com.cybzacg.blogbackend.module.article;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleCategoryService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.article.service.impl.PublicArticleServiceImpl;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.SysTagRelationService;
import com.cybzacg.blogbackend.module.content.service.SysTagService;
import com.cybzacg.blogbackend.module.content.service.UserFootprintService;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicArticleServiceImplTest {
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private BlogArticleCategoryService blogArticleCategoryService;
    @Mock
    private SysTagRelationService sysTagRelationService;
    @Mock
    private SysCategoryService sysCategoryService;
    @Mock
    private SysTagService sysTagService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysInteractionService sysInteractionService;
    @Mock
    private SysCollectionService sysCollectionService;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ArticleModelMapper articleModelMapper;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private UserFootprintService userFootprintService;
    @Mock
    private LambdaQueryChainWrapper<BlogArticle> articleQuery;
    @Mock
    private LambdaQueryChainWrapper<BlogArticleCategory> categoryQuery;
    @Mock
    private LambdaQueryChainWrapper<SysTagRelation> tagQuery;
    @Mock
    private LambdaQueryChainWrapper<SysInteraction> interactionQuery;
    @Mock
    private LambdaQueryChainWrapper<SysCollection> collectionQuery;

    private PublicArticleServiceImpl publicArticleService;

    @BeforeEach
    void setUp() {
        publicArticleService = new PublicArticleServiceImpl(
                blogArticleService,
                blogArticleCategoryService,
                sysTagRelationService,
                sysCategoryService,
                sysTagService,
                sysUserService,
                sysInteractionService,
                sysCollectionService,
                articleAccessControlService,
                articleModelMapper,
                contentModelMapper,
                userFootprintService
        );
        mockArticleMappings();
        mockContentMappings();
        lenient().when(articleAccessControlService.canAccessArticle(any(BlogArticle.class), any())).thenReturn(true);
    }

    @Test
    void pageArticlesShouldFilterByKeywordFromTitleAndSummary() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setKeyword("Java");

        BlogArticle titleMatched = article(1L, "Java 基础", "intro", 2, 101L);
        BlogArticle summaryMatched = article(2L, "Spring", "深入 Java 生态", 3, 102L);
        BlogArticle ignored = article(3L, "Go", "并发", 1, 103L);

        stubPublishedArticles(List.of(titleMatched, summaryMatched, ignored));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(101L, "u101", "作者甲"),
                user(102L, "u102", "作者乙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(2L, result.getTotal());
            assertIterableEquals(List.of(2L, 1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
            assertEquals("作者乙", result.getRecords().get(0).getAuthorName());
            assertEquals("作者甲", result.getRecords().get(1).getAuthorName());
        }
    }

    @Test
    void pageArticlesShouldFilterByCategory() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setCategoryId(10L);

        stubCategoryRelations(List.of(
                categoryRelation(2L, 10L, 1),
                categoryRelation(3L, 10L, 2)
        ));
        stubPublishedArticles(List.of(
                article(1L, "A1", "s1", 1, 101L),
                article(2L, "A2", "s2", 2, 102L),
                article(3L, "A3", "s3", 3, 103L)
        ));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(102L, "u102", "作者乙"),
                user(103L, "u103", "作者丙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(2L, result.getTotal());
            assertIterableEquals(List.of(3L, 2L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    @Test
    void pageArticlesShouldFilterByTag() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setTagId(20L);

        stubTagRelations(List.of(
                tagRelation(2L, 20L, 1L),
                tagRelation(4L, 20L, 2L)
        ));
        stubPublishedArticles(List.of(
                article(1L, "A1", "s1", 1, 101L),
                article(2L, "A2", "s2", 2, 102L),
                article(3L, "A3", "s3", 3, 103L),
                article(4L, "A4", "s4", 4, 104L)
        ));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(102L, "u102", "作者乙"),
                user(104L, "u104", "作者丁")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(2L, result.getTotal());
            assertIterableEquals(List.of(4L, 2L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    @Test
    void pageArticlesShouldFilterByCategoryAndTagIntersection() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setCategoryId(10L);
        query.setTagId(20L);

        stubCategoryRelations(List.of(
                categoryRelation(2L, 10L, 1),
                categoryRelation(3L, 10L, 2)
        ));
        stubTagRelations(List.of(
                tagRelation(3L, 20L, 1L),
                tagRelation(4L, 20L, 2L)
        ));
        stubPublishedArticles(List.of(
                article(1L, "A1", "s1", 1, 101L),
                article(2L, "A2", "s2", 2, 102L),
                article(3L, "A3", "s3", 3, 103L),
                article(4L, "A4", "s4", 4, 104L)
        ));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(user(103L, "u103", "作者丙")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertIterableEquals(List.of(3L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    @Test
    void pageArticlesShouldSortByLatest() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setSort("latest");

        stubPublishedArticles(List.of(
                article(1L, "A1", "s1", 1, 101L),
                article(2L, "A2", "s2", 3, 102L),
                article(3L, "A3", "s3", 2, 103L)
        ));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(101L, "u101", "作者甲"),
                user(102L, "u102", "作者乙"),
                user(103L, "u103", "作者丙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertIterableEquals(List.of(2L, 3L, 1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    @Test
    void pageArticlesShouldSortByTopFirstThenPublishTime() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setSort("top");

        BlogArticle topOld = article(1L, "A1", "s1", 1, 101L);
        topOld.setIsTop(1);
        BlogArticle normalNewest = article(2L, "A2", "s2", 4, 102L);
        normalNewest.setIsTop(0);
        BlogArticle topNewest = article(3L, "A3", "s3", 3, 103L);
        topNewest.setIsTop(1);
        stubPublishedArticles(List.of(topOld, normalNewest, topNewest));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(101L, "u101", "作者甲"),
                user(102L, "u102", "作者乙"),
                user(103L, "u103", "作者丙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertIterableEquals(List.of(3L, 1L, 2L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    @Test
    void pageArticlesShouldSortByHotScore() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setSort("hot");

        BlogArticle first = article(1L, "A1", "s1", 1, 101L);
        first.setViewCount(10);
        first.setLikeCount(2);
        first.setCommentCount(1);

        BlogArticle second = article(2L, "A2", "s2", 2, 102L);
        second.setViewCount(6);
        second.setLikeCount(4);
        second.setCommentCount(3);

        BlogArticle third = article(3L, "A3", "s3", 3, 103L);
        third.setViewCount(6);
        third.setLikeCount(4);
        third.setCommentCount(3);

        stubPublishedArticles(List.of(first, second, third));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(101L, "u101", "作者甲"),
                user(102L, "u102", "作者乙"),
                user(103L, "u103", "作者丙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertIterableEquals(List.of(3L, 2L, 1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
        }
    }

    @Test
    void pageArticlesShouldReturnEmptyWhenNoPublishedArticles() {
        stubPublishedArticles(List.of());

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(new PublicArticlePageQuery());

            assertEquals(0L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
            verify(sysUserService, never()).listByIds(anyCollection());
        }
    }

    @Test
    void pageArticlesShouldQueryPublishedStatusOnly() {
        stubPublishedArticles(List.of());

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            publicArticleService.pageArticles(new PublicArticlePageQuery());

            verify(articleQuery).eq(anySFunction(), eq(1));
        }
    }

    @Test
    void pageArticlesShouldReturnEmptyRecordsWhenPageOutOfRange() {
        PublicArticlePageQuery query = new PublicArticlePageQuery();
        query.setCurrent(3L);
        query.setSize(10L);

        stubPublishedArticles(List.of(article(1L, "A1", "s1", 1, 101L)));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(user(101L, "u101", "作者甲")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(query);

            assertEquals(1L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Test
    void pageArticlesShouldFilterUnavailableArticlesForAnonymousUser() {
        BlogArticle publicArticle = article(1L, "公开", "s1", 1, 101L);
        BlogArticle loginArticle = article(2L, "登录可见", "s2", 2, 102L);
        loginArticle.setAccessLevel(1);
        BlogArticle specifiedArticle = article(3L, "指定用户", "s3", 3, 103L);
        specifiedArticle.setAccessLevel(4);

        stubPublishedArticles(List.of(publicArticle, loginArticle, specifiedArticle));
        when(articleAccessControlService.canAccessArticle(any(BlogArticle.class), any())).thenAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            Long userId = invocation.getArgument(1);
            return userId == null && article.getId().equals(1L);
        });
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(user(101L, "u101", "作者甲")));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(new PublicArticlePageQuery());

            assertEquals(1L, result.getTotal());
            assertIterableEquals(List.of(1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
            verify(articleAccessControlService).canAccessArticle(loginArticle, null);
            verify(articleAccessControlService).canAccessArticle(specifiedArticle, null);
        }
    }

    @Test
    void pageArticlesShouldIncludeSpecifiedArticleForAuthorizedUser() {
        BlogArticle publicArticle = article(1L, "公开", "s1", 1, 101L);
        BlogArticle specifiedArticle = article(3L, "指定用户", "s3", 3, 103L);
        specifiedArticle.setAccessLevel(4);

        stubPublishedArticles(List.of(publicArticle, specifiedArticle));
        when(articleAccessControlService.canAccessArticle(any(BlogArticle.class), any())).thenAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            Long userId = invocation.getArgument(1);
            return article.getId().equals(1L) || (article.getId().equals(3L) && Long.valueOf(9L).equals(userId));
        });
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(101L, "u101", "作者甲"),
                user(103L, "u103", "作者丙")
        ));

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(9L)) {
            PageResult<PublicArticleCardVO> result = publicArticleService.pageArticles(new PublicArticlePageQuery());

            assertEquals(2L, result.getTotal());
            assertIterableEquals(List.of(3L, 1L), result.getRecords().stream().map(PublicArticleCardVO::getId).toList());
            verify(articleAccessControlService).canAccessArticle(specifiedArticle, 9L);
        }
    }

    @Test
    void getArticleShouldThrowWhenArticleDoesNotExist() {
        when(blogArticleService.getById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> publicArticleService.getArticle(99L));

        assertEquals(ResultErrorCode.NO_HANDLER_FOUND.getCode(), exception.getCode());
        assertEquals("文章不存在", exception.getMessage());
        verifyNoInteractions(userFootprintService);
    }

    @Test
    void getArticleShouldRejectUnpublishedArticle() {
        BlogArticle draft = article(10L, "草稿", "未发布", 1, 101L);
        draft.setStatus(0);
        when(blogArticleService.getById(10L)).thenReturn(draft);

        BusinessException exception = assertThrows(BusinessException.class, () -> publicArticleService.getArticle(10L));

        assertEquals(ResultErrorCode.NO_HANDLER_FOUND.getCode(), exception.getCode());
        assertEquals("文章不存在", exception.getMessage());
        verifyNoInteractions(articleAccessControlService, userFootprintService);
    }

    @Test
    void getArticleShouldRequireLoginForLoginOnlyArticle() {
        BlogArticle article = article(11L, "登录可见", "summary", 1, 101L);
        article.setAccessLevel(1);
        when(blogArticleService.getById(11L)).thenReturn(article);

        try (MockedStatic<?> ignoredSecurity = SecurityTestUtils.mockUserId(null)) {
            BusinessException exception = assertThrows(BusinessException.class, () -> publicArticleService.getArticle(11L));

            assertEquals(ResultErrorCode.LOGIN_REQUIRED.getCode(), exception.getCode());
            verify(articleAccessControlService, never()).validateArticleAccess(any(BlogArticle.class), any());
            verifyNoInteractions(userFootprintService);
        }
    }

    @Test
    void getArticleShouldAssembleDetailAndRecordFootprint() {
        BlogArticle article = article(12L, "文章详情", "摘要", 5, 101L);
        article.setContent("content");
        article.setAccessLevel(0);
        when(blogArticleService.getById(12L)).thenReturn(article);
        when(sysUserService.getById(101L)).thenReturn(user(101L, "author101", "作者甲"));

        stubCategoryRelations(List.of(
                categoryRelation(12L, 1001L, 1),
                categoryRelation(12L, 1002L, 2)
        ));
        when(sysCategoryService.listByIds(List.of(1001L, 1002L))).thenReturn(List.of(
                category(1001L, "后端"),
                category(1002L, "Java")
        ));

        stubTagRelations(List.of(
                tagRelation(12L, 2001L, 1L),
                tagRelation(12L, 2002L, 2L)
        ));
        when(sysTagService.listByIds(List.of(2001L, 2002L))).thenReturn(List.of(
                tag(2001L, "Spring"),
                tag(2002L, "Boot")
        ));

        when(sysInteractionService.lambdaQuery()).thenReturn(interactionQuery);
        when(interactionQuery.eq(anySFunction(), any())).thenReturn(interactionQuery);
        when(interactionQuery.exists()).thenReturn(true);

        when(sysCollectionService.lambdaQuery()).thenReturn(collectionQuery);
        when(collectionQuery.eq(anySFunction(), any())).thenReturn(collectionQuery);
        when(collectionQuery.exists()).thenReturn(true);

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

    private void stubPublishedArticles(List<BlogArticle> articles) {
        when(blogArticleService.lambdaQuery()).thenReturn(articleQuery);
        when(articleQuery.eq(anySFunction(), any())).thenReturn(articleQuery);
        when(articleQuery.list()).thenReturn(articles);
    }

    private void stubCategoryRelations(List<BlogArticleCategory> relations) {
        when(blogArticleCategoryService.lambdaQuery()).thenReturn(categoryQuery);
        when(categoryQuery.eq(anySFunction(), any())).thenReturn(categoryQuery);
        lenient().when(categoryQuery.orderByAsc(anySFunction())).thenReturn(categoryQuery);
        when(categoryQuery.list()).thenReturn(relations);
    }

    private void stubTagRelations(List<SysTagRelation> relations) {
        when(sysTagRelationService.lambdaQuery()).thenReturn(tagQuery);
        when(tagQuery.eq(anySFunction(), any())).thenReturn(tagQuery);
        lenient().when(tagQuery.orderByAsc(anySFunction())).thenReturn(tagQuery);
        when(tagQuery.list()).thenReturn(relations);
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
        article.setPublishTime(new Date(1_000L * publishTick));
        return article;
    }

    private BlogArticleCategory categoryRelation(Long articleId, Long categoryId, Integer sortOrder) {
        BlogArticleCategory relation = new BlogArticleCategory();
        relation.setArticleId(articleId);
        relation.setCategoryId(categoryId);
        relation.setSortOrder(sortOrder);
        return relation;
    }

    private SysTagRelation tagRelation(Long targetId, Long tagId, Long id) {
        SysTagRelation relation = new SysTagRelation();
        relation.setId(id);
        relation.setTargetId(targetId);
        relation.setTagId(tagId);
        relation.setTargetType("article");
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

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
