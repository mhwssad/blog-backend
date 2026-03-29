package com.cybzacg.blogbackend.module.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.convert.ArticleModelMapper;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private ArticleAdminServiceImpl articleAdminService;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void deleteArticleShouldCleanupAttachmentsCommentInteractionsAndFolderCounts() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);

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
        when(fileBusinessInfoService.removeByIds(List.of(401L))).thenReturn(true);
        when(sysCollectionFolderService.getById(301L)).thenReturn(folder);
        when(sysCollectionFolderService.updateById(folder)).thenReturn(true);
        when(blogArticleAccessService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(blogArticleCategoryService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(sysTagRelationService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(sysCommentService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(sysCollectionService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(sysInteractionService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(sysUserFootprintService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(blogArticleService.removeById(1L)).thenReturn(true);

        articleAdminService.deleteArticle(1L);

        verify(fileBusinessInfoService).removeByIds(List.of(401L));
        verify(fileLifecycleService).syncFileAfterReferenceRemoval(501L);
        verify(sysCollectionFolderService).updateById(folder);
        verify(sysInteractionService, times(2)).remove(any(LambdaQueryWrapper.class));
        verify(blogArticleService).removeById(1L);
        assertEquals(Integer.valueOf(0), folder.getCollectionCount());
    }

    @Test
    void updateStatusShouldSetPublishTimeWhenPublishingDraft() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setStatus(0);
        article.setPublishTime(null);

        when(blogArticleService.getById(1L)).thenReturn(article);
        when(blogArticleService.updateById(article)).thenReturn(true);

        articleAdminService.updateStatus(1L, 1);

        assertEquals(Integer.valueOf(1), article.getStatus());
        assertNotNull(article.getPublishTime());
        verify(blogArticleService).updateById(article);
    }

    @Test
    void assignAccessShouldRejectWhenArticleAccessLevelIsNotSpecifiedUser() {
        BlogArticle article = new BlogArticle();
        article.setId(2L);
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
        BlogArticle article = new BlogArticle();
        article.setId(3L);
        article.setAccessLevel(4);

        ArticleAccessItem accessItem = new ArticleAccessItem();
        accessItem.setUserId(9L);

        SysUser user = new SysUser();
        user.setId(9L);
        user.setDeletedFlag(0);

        when(blogArticleService.getById(3L)).thenReturn(article);
        when(sysUserService.listByIds(any())).thenReturn(List.of(user));
        when(blogArticleAccessService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(blogArticleAccessService.saveBatch(any())).thenReturn(true);

        articleAdminService.assignAccess(3L, List.of(accessItem));

        assertEquals(Integer.valueOf(1), accessItem.getAccessType());
        verify(blogArticleAccessService).remove(any(LambdaQueryWrapper.class));
        verify(blogArticleAccessService).saveBatch(any());
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
