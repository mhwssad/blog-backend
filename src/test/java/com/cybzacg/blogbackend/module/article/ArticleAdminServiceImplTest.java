package com.cybzacg.blogbackend.module.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleAccessService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleCategoryService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.article.service.impl.ArticleAdminServiceImpl;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionFolderService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import com.cybzacg.blogbackend.module.content.service.SysTagRelationService;
import com.cybzacg.blogbackend.module.content.service.SysTagService;
import com.cybzacg.blogbackend.module.content.service.SysUserFootprintService;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleAdminServiceImplTest {
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private BlogArticleCategoryService blogArticleCategoryService;
    @Mock
    private BlogArticleAccessService blogArticleAccessService;
    @Mock
    private SysTagRelationService sysTagRelationService;
    @Mock
    private SysCategoryService sysCategoryService;
    @Mock
    private SysTagService sysTagService;
    @Mock
    private SysCommentService sysCommentService;
    @Mock
    private SysCollectionFolderService sysCollectionFolderService;
    @Mock
    private SysCollectionService sysCollectionService;
    @Mock
    private SysInteractionService sysInteractionService;
    @Mock
    private SysUserFootprintService sysUserFootprintService;
    @Mock
    private FileBusinessInfoService fileBusinessInfoService;
    @Mock
    private FileLifecycleService fileLifecycleService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private ArticleModelMapper articleModelMapper;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private LambdaQueryChainWrapper<SysComment> commentQuery;
    @Mock
    private LambdaQueryChainWrapper<SysCollection> articleCollectionQuery;
    @Mock
    private LambdaQueryChainWrapper<SysCollection> folderCollectionCountQuery;
    @Mock
    private LambdaQueryChainWrapper<FileBusinessInfo> attachmentQuery;
    @Mock
    private LambdaQueryChainWrapper<SysCategory> categoryValidationQuery;
    @Mock
    private LambdaQueryChainWrapper<BlogArticleCategory> categoryListQuery;
    @Mock
    private LambdaQueryChainWrapper<SysTagRelation> tagListQuery;

    private ArticleAdminServiceImpl articleAdminService;

    @BeforeEach
    void setUp() {
        initTableInfo(BlogArticle.class);
        articleAdminService = new ArticleAdminServiceImpl(
                blogArticleService,
                blogArticleCategoryService,
                blogArticleAccessService,
                sysTagRelationService,
                sysCategoryService,
                sysTagService,
                sysCommentService,
                sysCollectionFolderService,
                sysCollectionService,
                sysInteractionService,
                sysUserFootprintService,
                fileBusinessInfoService,
                fileLifecycleService,
                sysUserService,
                articleModelMapper,
                articleAccessControlService
        );
        mockMapperDefaults();
        lenient().when(blogArticleCategoryService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        lenient().when(blogArticleCategoryService.saveBatch(anyCollection())).thenReturn(true);
        lenient().when(sysTagRelationService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        lenient().when(sysTagRelationService.saveBatch(anyCollection())).thenReturn(true);
        lenient().when(blogArticleAccessService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        lenient().when(blogArticleAccessService.saveBatch(anyCollection())).thenReturn(true);
        lenient().when(blogArticleService.updateById(any(BlogArticle.class))).thenReturn(true);
        lenient().when(blogArticleService.removeById(anyLong())).thenReturn(true);
        lenient().when(sysCollectionFolderService.updateById(any(SysCollectionFolder.class))).thenReturn(true);
        lenient().when(sysCommentService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        lenient().when(sysCollectionService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        lenient().when(sysInteractionService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        lenient().when(sysUserFootprintService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        lenient().when(fileBusinessInfoService.removeByIds(anyCollection())).thenReturn(true);
        lenient().doAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            if (article.getId() == null) {
                article.setId(100L);
            }
            return true;
        }).when(blogArticleService).save(any(BlogArticle.class));
    }

    @Test
    void deleteArticleShouldCleanupAttachmentsCommentInteractionsAndFolderCounts() {
        BlogArticle article = article(1L, "待删除文章", 1L, 1, 0);

        SysComment comment = new SysComment();
        comment.setId(101L);
        comment.setTargetType("article");
        comment.setTargetId(1L);

        SysCollection collection = new SysCollection();
        collection.setId(201L);
        collection.setFolderId(301L);
        collection.setTargetType("article");
        collection.setTargetId(1L);

        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(401L);
        reference.setFileId(501L);
        reference.setReferenceType("article_attachment");
        reference.setReferenceId(1L);

        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(301L);
        folder.setCollectionCount(1);

        when(blogArticleService.getById(1L)).thenReturn(article);
        when(sysCommentService.lambdaQuery()).thenReturn(commentQuery);
        when(commentQuery.eq(anySFunction(), any())).thenReturn(commentQuery);
        when(commentQuery.list()).thenReturn(List.of(comment));
        when(sysCollectionService.lambdaQuery()).thenReturn(articleCollectionQuery, folderCollectionCountQuery);
        when(articleCollectionQuery.eq(anySFunction(), any())).thenReturn(articleCollectionQuery);
        when(articleCollectionQuery.list()).thenReturn(List.of(collection));
        when(folderCollectionCountQuery.eq(anySFunction(), any())).thenReturn(folderCollectionCountQuery);
        when(folderCollectionCountQuery.count()).thenReturn(0L);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(attachmentQuery);
        when(attachmentQuery.eq(anySFunction(), any())).thenReturn(attachmentQuery);
        when(attachmentQuery.list()).thenReturn(List.of(reference));
        when(sysCollectionFolderService.getById(301L)).thenReturn(folder);

        articleAdminService.deleteArticle(1L);

        verify(fileBusinessInfoService).removeByIds(List.of(401L));
        verify(fileLifecycleService).syncFileAfterReferenceRemoval(501L);
        verify(sysCollectionFolderService).updateById(folder);
        verify(sysInteractionService, times(2)).remove(any(LambdaQueryWrapper.class));
        verify(blogArticleService).removeById(1L);
        assertEquals(0, folder.getCollectionCount());
    }

    @Test
    void updateStatusShouldSetPublishTimeWhenPublishingDraft() {
        BlogArticle article = article(1L, "草稿", 1L, 0, 0);
        article.setPublishTime(null);

        when(blogArticleService.getById(1L)).thenReturn(article);

        articleAdminService.updateStatus(1L, 1);

        assertEquals(1, article.getStatus());
        assertNotNull(article.getPublishTime());
        verify(blogArticleService).updateById(article);
    }

    @Test
    void assignAccessShouldRejectWhenArticleAccessLevelIsNotSpecifiedUser() {
        BlogArticle article = article(2L, "普通文章", 1L, 1, 0);
        article.setAccessLevel(0);
        when(blogArticleService.getById(2L)).thenReturn(article);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> articleAdminService.assignAccess(2L, List.of(new ArticleAccessItem()))
        );

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("当前文章访问级别不是指定用户可见", exception.getMessage());
        verifyNoInteractions(sysUserService);
    }

    @Test
    void assignAccessShouldRebuildDistinctUserBindingsForSpecifiedArticle() {
        BlogArticle article = article(3L, "指定访问文章", 1L, 1, 4);
        ArticleAccessItem accessItem = accessItem(9L, 1);
        SysUser user = user(9L, "user9", "用户9");

        when(blogArticleService.getById(3L)).thenReturn(article);
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(user));

        articleAdminService.assignAccess(3L, List.of(accessItem));

        assertEquals(1, accessItem.getAccessType());
        verify(blogArticleAccessService).remove(any(LambdaQueryWrapper.class));
        verify(blogArticleAccessService).saveBatch(anyCollection());
    }

    @Test
    void createArticleShouldCreateWithCategoryTagAndAccessBindings() {
        ArticleSaveRequest request = saveRequest(1L, 1, 4, List.of(11L, 12L), List.of(21L, 22L), List.of(accessItem(9L, 1)));
        when(sysUserService.getById(1L)).thenReturn(user(1L, "author", "作者甲"));
        stubCategoryValidation(List.of(category(11L, "后端"), category(12L, "Java")));
        when(sysTagService.listByIds(List.of(21L, 22L))).thenReturn(List.of(tag(21L, "Spring"), tag(22L, "Boot")));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(user(9L, "reader9", "读者9")));
        stubDetailLookups(100L, 1L, List.of(11L, 12L), List.of(21L, 22L), List.of(accessRecord(100L, 9L, 1)));

        ArgumentCaptor<BlogArticle> articleCaptor = ArgumentCaptor.forClass(BlogArticle.class);

        ArticleDetailVO detail = articleAdminService.createArticle(request);

        verify(blogArticleService).save(articleCaptor.capture());
        BlogArticle savedArticle = articleCaptor.getValue();
        assertEquals(100L, detail.getId());
        assertEquals("文章标题", detail.getTitle());
        assertEquals(List.of(11L, 12L), detail.getCategoryIds());
        assertEquals(List.of(21L, 22L), detail.getTagIds());
        assertEquals(1, detail.getAccessList().size());
        assertEquals("作者甲", detail.getAuthorName());
        assertEquals(0, savedArticle.getLikeCount());
        assertEquals(0, savedArticle.getCommentCount());
        assertEquals(4, savedArticle.getAccessLevel());
        assertNotNull(savedArticle.getPublishTime());
        verify(blogArticleCategoryService).saveBatch(anyCollection());
        verify(sysTagRelationService).saveBatch(anyCollection());
        verify(blogArticleAccessService).saveBatch(anyCollection());
    }

    @Test
    void createArticleShouldAllowEmptyCategoryAndTagBindings() {
        ArticleSaveRequest request = saveRequest(1L, 0, 0, List.of(), List.of(), List.of());
        when(sysUserService.getById(1L)).thenReturn(user(1L, "author", "作者甲"));
        stubDetailLookups(100L, 1L, List.of(), List.of(), List.of());

        ArticleDetailVO detail = articleAdminService.createArticle(request);

        assertTrue(detail.getCategoryIds().isEmpty());
        assertTrue(detail.getTagIds().isEmpty());
        assertTrue(detail.getAccessList().isEmpty());
        verify(blogArticleCategoryService, never()).saveBatch(anyCollection());
        verify(sysTagRelationService, never()).saveBatch(anyCollection());
        verify(blogArticleAccessService, never()).saveBatch(anyCollection());
        verify(blogArticleAccessService).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    void updateArticleShouldUpdateAndRebuildRelations() {
        BlogArticle existing = article(200L, "旧标题", 1L, 0, 0);
        existing.setAuthorId(1L);
        when(blogArticleService.getById(200L)).thenReturn(existing);

        ArticleSaveRequest request = saveRequest(2L, 1, 4, List.of(31L, 32L), List.of(41L), List.of(accessItem(9L, 2)));
        request.setTitle("新标题");
        request.setSummary("新摘要");
        request.setContent("新内容");
        when(sysUserService.getById(2L)).thenReturn(user(2L, "author2", "作者乙"));
        stubCategoryValidation(List.of(category(31L, "后端"), category(32L, "Spring")));
        when(sysTagService.listByIds(List.of(41L))).thenReturn(List.of(tag(41L, "测试")));
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(user(9L, "reader9", "读者9")));
        stubDetailLookups(200L, 2L, List.of(31L, 32L), List.of(41L), List.of(accessRecord(200L, 9L, 2)));

        ArticleDetailVO detail = articleAdminService.updateArticle(200L, request);

        assertEquals(200L, detail.getId());
        assertEquals("新标题", detail.getTitle());
        assertEquals(List.of(31L, 32L), detail.getCategoryIds());
        assertEquals(List.of(41L), detail.getTagIds());
        assertEquals(1, detail.getAccessList().size());
        assertEquals("作者乙", detail.getAuthorName());
        verify(blogArticleService).updateById(existing);
        verify(blogArticleCategoryService).saveBatch(anyCollection());
        verify(sysTagRelationService).saveBatch(anyCollection());
        verify(blogArticleAccessService).saveBatch(anyCollection());
    }

    @Test
    void updateArticleShouldClearCategoryAndTagBindings() {
        BlogArticle existing = article(201L, "旧标题", 1L, 1, 4);
        existing.setAuthorId(1L);
        when(blogArticleService.getById(201L)).thenReturn(existing);

        ArticleSaveRequest request = saveRequest(1L, 0, 0, List.of(), List.of(), List.of());
        request.setTitle("清空绑定");
        when(sysUserService.getById(1L)).thenReturn(user(1L, "author", "作者甲"));
        stubDetailLookups(201L, 1L, List.of(), List.of(), List.of());

        ArticleDetailVO detail = articleAdminService.updateArticle(201L, request);

        assertTrue(detail.getCategoryIds().isEmpty());
        assertTrue(detail.getTagIds().isEmpty());
        assertTrue(detail.getAccessList().isEmpty());
        verify(blogArticleService).updateById(existing);
        verify(blogArticleCategoryService, never()).saveBatch(anyCollection());
        verify(sysTagRelationService, never()).saveBatch(anyCollection());
        verify(blogArticleAccessService, never()).saveBatch(anyCollection());
        verify(blogArticleAccessService).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    void updateArticleShouldRemoveSpecifiedAccessBindingsWhenDowngradingToPublic() {
        BlogArticle existing = article(202L, "旧标题", 1L, 1, 4);
        existing.setAuthorId(1L);
        when(blogArticleService.getById(202L)).thenReturn(existing);

        ArticleSaveRequest request = saveRequest(1L, 1, 0, List.of(71L), List.of(81L), List.of());
        request.setTitle("降级公开");
        when(sysUserService.getById(1L)).thenReturn(user(1L, "author", "作者甲"));
        stubCategoryValidation(List.of(category(71L, "公开分类")));
        when(sysTagService.listByIds(List.of(81L))).thenReturn(List.of(tag(81L, "公开标签")));
        stubDetailLookups(202L, 1L, List.of(71L), List.of(81L), List.of());

        ArticleDetailVO detail = articleAdminService.updateArticle(202L, request);

        assertEquals(0, detail.getAccessLevel());
        assertEquals(List.of(71L), detail.getCategoryIds());
        assertEquals(List.of(81L), detail.getTagIds());
        assertTrue(detail.getAccessList().isEmpty());
        verify(blogArticleService).updateById(existing);
        verify(blogArticleCategoryService).saveBatch(anyCollection());
        verify(sysTagRelationService).saveBatch(anyCollection());
        verify(blogArticleAccessService, never()).saveBatch(anyCollection());
        verify(blogArticleAccessService).remove(any(LambdaQueryWrapper.class));
    }

    @Test
    void pageArticlesShouldReturnPagedRecords() {
        ArticleAdminPageQuery query = new ArticleAdminPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);

        BlogArticle first = article(1L, "文章一", 1L, 1, 0);
        first.setAuthorId(11L);
        BlogArticle second = article(2L, "文章二", 2L, 1, 0);
        second.setAuthorId(12L);
        Page<BlogArticle> page = new Page<>(2, 5);
        page.setTotal(12);
        page.setRecords(List.of(first, second));
        when(blogArticleService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user(11L, "u11", "作者一"),
                user(12L, "u12", "作者二")
        ));

        PageResult<ArticleAdminVO> result = articleAdminService.pageArticles(query);

        assertEquals(12L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(List.of(1L, 2L), result.getRecords().stream().map(ArticleAdminVO::getId).toList());
        assertEquals("作者一", result.getRecords().get(0).getAuthorName());
        assertEquals("作者二", result.getRecords().get(1).getAuthorName());
    }

    @Test
    void pageArticlesShouldApplyStatusAuthorAndKeywordFilters() {
        ArticleAdminPageQuery query = new ArticleAdminPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);
        query.setKeyword("Java");
        query.setAuthorId(7L);
        query.setStatus(1);

        Page<BlogArticle> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());
        when(blogArticleService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        ArgumentCaptor<Page<BlogArticle>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<BlogArticle>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);

        PageResult<ArticleAdminVO> result = articleAdminService.pageArticles(query);

        assertTrue(result.getRecords().isEmpty());
        verify(blogArticleService).page(pageCaptor.capture(), wrapperCaptor.capture());
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(10L, pageCaptor.getValue().getSize());
        Collection<Object> params = wrapperCaptor.getValue().getParamNameValuePairs().values();
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("LIKE"));
        assertTrue(params.contains(7L));
        assertTrue(params.contains(1));
    }

    @Test
    void getArticleShouldThrowWhenNotFound() {
        when(blogArticleService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> articleAdminService.getArticle(999L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("文章不存在", exception.getMessage());
    }

    @Test
    void getArticleShouldReturnDetailWithCategoryTagAndAccessList() {
        BlogArticle article = article(300L, "详情文章", 1L, 1, 4);
        article.setAuthorId(1L);
        when(blogArticleService.getById(300L)).thenReturn(article);
        stubDetailLookups(300L, 1L, List.of(51L, 52L), List.of(61L, 62L), List.of(
                accessRecord(300L, 9L, 1),
                accessRecord(300L, 10L, 2)
        ));

        ArticleDetailVO detail = articleAdminService.getArticle(300L);

        assertEquals(300L, detail.getId());
        assertEquals("详情文章", detail.getTitle());
        assertEquals("作者甲", detail.getAuthorName());
        assertEquals(List.of(51L, 52L), detail.getCategoryIds());
        assertEquals(List.of(61L, 62L), detail.getTagIds());
        assertEquals(2, detail.getAccessList().size());
        assertEquals(9L, detail.getAccessList().get(0).getUserId());
    }

    private void mockMapperDefaults() {
        lenient().when(articleModelMapper.toAdminVO(any(BlogArticle.class))).thenAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            ArticleAdminVO vo = new ArticleAdminVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setAuthorId(article.getAuthorId());
            vo.setStatus(article.getStatus());
            vo.setAccessLevel(article.getAccessLevel());
            vo.setPublishTime(article.getPublishTime());
            vo.setUpdatedAt(article.getUpdatedAt());
            return vo;
        });
        lenient().when(articleModelMapper.toDetailVO(any(BlogArticle.class))).thenAnswer(invocation -> {
            BlogArticle article = invocation.getArgument(0);
            ArticleDetailVO vo = new ArticleDetailVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setContent(article.getContent());
            vo.setAuthorId(article.getAuthorId());
            vo.setStatus(article.getStatus());
            vo.setAccessLevel(article.getAccessLevel());
            vo.setPublishTime(article.getPublishTime());
            vo.setRemark(article.getRemark());
            return vo;
        });
        lenient().when(articleModelMapper.toArticle(any(ArticleSaveRequest.class))).thenAnswer(invocation -> mappedArticle(invocation.getArgument(0)));
        lenient().doAnswer(invocation -> {
            ArticleSaveRequest request = invocation.getArgument(0);
            BlogArticle article = invocation.getArgument(1);
            applyRequest(article, request);
            return null;
        }).when(articleModelMapper).updateArticle(any(ArticleSaveRequest.class), any(BlogArticle.class));
        lenient().when(articleModelMapper.toArticleCategory(anyLong(), anyLong(), any(Integer.class))).thenAnswer(invocation -> {
            BlogArticleCategory relation = new BlogArticleCategory();
            relation.setArticleId(invocation.getArgument(0));
            relation.setCategoryId(invocation.getArgument(1));
            relation.setSortOrder(invocation.getArgument(2));
            return relation;
        });
        lenient().when(articleModelMapper.toTagRelation(anyLong(), anyLong(), anyString())).thenAnswer(invocation -> {
            SysTagRelation relation = new SysTagRelation();
            relation.setTagId(invocation.getArgument(0));
            relation.setTargetId(invocation.getArgument(1));
            relation.setTargetType(invocation.getArgument(2));
            return relation;
        });
        lenient().when(articleModelMapper.toAccessItem(any(BlogArticleAccess.class))).thenAnswer(invocation -> {
            BlogArticleAccess access = invocation.getArgument(0);
            ArticleAccessItem item = new ArticleAccessItem();
            item.setUserId(access.getUserId());
            item.setAccessType(access.getAccessType());
            item.setExpireTime(access.getExpireTime());
            item.setGrantReason(access.getGrantReason());
            return item;
        });
        lenient().when(articleModelMapper.toArticleAccess(anyLong(), any(ArticleAccessItem.class), any(Date.class))).thenAnswer(invocation -> {
            Long articleId = invocation.getArgument(0);
            ArticleAccessItem item = invocation.getArgument(1);
            Date grantTime = invocation.getArgument(2);
            BlogArticleAccess access = new BlogArticleAccess();
            access.setArticleId(articleId);
            access.setUserId(item.getUserId());
            access.setAccessType(item.getAccessType());
            access.setGrantTime(grantTime);
            access.setExpireTime(item.getExpireTime());
            access.setGrantReason(item.getGrantReason());
            return access;
        });
    }

    private void stubCategoryValidation(List<SysCategory> categories) {
        when(sysCategoryService.lambdaQuery()).thenReturn(categoryValidationQuery);
        when(categoryValidationQuery.in(anySFunction(), anyCollection())).thenReturn(categoryValidationQuery);
        when(categoryValidationQuery.eq(anySFunction(), any())).thenReturn(categoryValidationQuery);
        when(categoryValidationQuery.list()).thenReturn(categories);
    }

    private void stubDetailLookups(Long articleId,
                                   Long authorId,
                                   List<Long> categoryIds,
                                   List<Long> tagIds,
                                   List<BlogArticleAccess> accesses) {
        when(sysUserService.getById(authorId)).thenReturn(user(authorId, "author-" + authorId, defaultAuthorName(authorId)));

        when(blogArticleCategoryService.lambdaQuery()).thenReturn(categoryListQuery);
        when(categoryListQuery.eq(anySFunction(), any())).thenReturn(categoryListQuery);
        when(categoryListQuery.orderByAsc(anySFunction())).thenReturn(categoryListQuery);
        when(categoryListQuery.list()).thenReturn(categoryIds.stream()
                .map(categoryId -> {
                    BlogArticleCategory relation = new BlogArticleCategory();
                    relation.setArticleId(articleId);
                    relation.setCategoryId(categoryId);
                    return relation;
                })
                .toList());

        when(sysTagRelationService.lambdaQuery()).thenReturn(tagListQuery);
        when(tagListQuery.eq(anySFunction(), any())).thenReturn(tagListQuery);
        when(tagListQuery.orderByAsc(anySFunction())).thenReturn(tagListQuery);
        when(tagListQuery.list()).thenReturn(tagIds.stream()
                .map(tagId -> {
                    SysTagRelation relation = new SysTagRelation();
                    relation.setTargetId(articleId);
                    relation.setTargetType("article");
                    relation.setTagId(tagId);
                    return relation;
                })
                .toList());

        when(articleAccessControlService.listArticleAccesses(articleId)).thenReturn(accesses);
    }

    private BlogArticle mappedArticle(ArticleSaveRequest request) {
        BlogArticle article = new BlogArticle();
        applyRequest(article, request);
        return article;
    }

    private void applyRequest(BlogArticle article, ArticleSaveRequest request) {
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setAuthorId(request.getAuthorId());
        article.setIsTop(request.getIsTop());
        article.setIsOriginal(request.getIsOriginal());
        article.setSourceUrl(request.getSourceUrl());
        article.setStatus(request.getStatus());
        article.setPublishTime(request.getPublishTime());
        article.setAccessLevel(request.getAccessLevel());
        article.setRemark(request.getRemark());
    }

    private ArticleSaveRequest saveRequest(Long authorId,
                                           Integer status,
                                           Integer accessLevel,
                                           List<Long> categoryIds,
                                           List<Long> tagIds,
                                           List<ArticleAccessItem> accessList) {
        ArticleSaveRequest request = new ArticleSaveRequest();
        request.setTitle("文章标题");
        request.setSummary("文章摘要");
        request.setContent("文章内容");
        request.setAuthorId(authorId);
        request.setIsTop(0);
        request.setIsOriginal(1);
        request.setStatus(status);
        request.setAccessLevel(accessLevel);
        request.setCategoryIds(categoryIds);
        request.setTagIds(tagIds);
        request.setAccessList(accessList);
        request.setRemark("备注");
        return request;
    }

    private BlogArticle article(Long id, String title, Long authorId, Integer status, Integer accessLevel) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setTitle(title);
        article.setSummary(title + "-summary");
        article.setContent(title + "-content");
        article.setAuthorId(authorId);
        article.setStatus(status);
        article.setAccessLevel(accessLevel);
        article.setIsTop(0);
        article.setIsOriginal(1);
        article.setPublishTime(new Date(1_000L * id));
        article.setUpdatedAt(new Date(2_000L * id));
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setCollectCount(0);
        article.setShareCount(0);
        article.setViewCount(0);
        return article;
    }

    private ArticleAccessItem accessItem(Long userId, Integer accessType) {
        ArticleAccessItem item = new ArticleAccessItem();
        item.setUserId(userId);
        item.setAccessType(accessType);
        item.setGrantReason("测试授权");
        return item;
    }

    private BlogArticleAccess accessRecord(Long articleId, Long userId, Integer accessType) {
        BlogArticleAccess access = new BlogArticleAccess();
        access.setArticleId(articleId);
        access.setUserId(userId);
        access.setAccessType(accessType);
        access.setGrantReason("测试授权");
        return access;
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
        category.setType("article");
        return category;
    }

    private SysTag tag(Long id, String name) {
        SysTag tag = new SysTag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    private String defaultAuthorName(Long authorId) {
        if (Long.valueOf(2L).equals(authorId)) {
            return "作者乙";
        }
        return "作者甲";
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        assistant.setCurrentNamespace(entityClass.getName());
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
