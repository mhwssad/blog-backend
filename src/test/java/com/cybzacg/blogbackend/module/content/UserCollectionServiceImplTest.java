package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.service.SysCollectionFolderService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import com.cybzacg.blogbackend.module.content.service.impl.UserCollectionServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCollectionServiceImplTest {
    @Mock
    private SysCollectionFolderService sysCollectionFolderService;
    @Mock
    private SysCollectionService sysCollectionService;
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysCollectionFolder> folderLookupQuery;
    @Mock
    private LambdaQueryChainWrapper<SysCollectionFolder> defaultFolderCleanupQuery;
    @Mock
    private LambdaQueryChainWrapper<SysCollection> collectionExistsQuery;

    private UserCollectionServiceImpl userCollectionService;

    @BeforeEach
    void setUp() {
        userCollectionService = new UserCollectionServiceImpl(
                sysCollectionFolderService,
                sysCollectionService,
                blogArticleService,
                articleAccessControlService,
                contentModelMapper
        );
    }

    @Test
    void createCollectionShouldCreateDefaultFolderAndSyncCounts() {
        CollectionSaveRequest request = new CollectionSaveRequest();
        request.setTargetId(10L);
        request.setTargetType("article");
        request.setRemark(" read later ");

        BlogArticle article = new BlogArticle();
        article.setId(10L);
        article.setStatus(1);
        article.setTitle("MapStruct Article");
        article.setCollectCount(2);

        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setUserId(7L);
        folder.setFolderType("article");
        folder.setIsDefault(1);
        folder.setCollectionCount(0);

        SysCollection collection = new SysCollection();
        collection.setUserId(7L);
        collection.setTargetId(10L);
        collection.setTargetType("article");

        when(blogArticleService.getById(10L)).thenReturn(article);
        when(sysCollectionFolderService.lambdaQuery()).thenReturn(folderLookupQuery, defaultFolderCleanupQuery);
        when(folderLookupQuery.eq(anySFunction(), any())).thenReturn(folderLookupQuery);
        when(folderLookupQuery.one()).thenReturn(null);
        when(defaultFolderCleanupQuery.eq(anySFunction(), any())).thenReturn(defaultFolderCleanupQuery);
        when(defaultFolderCleanupQuery.list()).thenReturn(List.of());
        when(sysCollectionService.lambdaQuery()).thenReturn(collectionExistsQuery);
        when(collectionExistsQuery.eq(anySFunction(), any())).thenReturn(collectionExistsQuery);
        when(collectionExistsQuery.exists()).thenReturn(false);
        when(contentModelMapper.toDefaultCollectionFolder(7L, "article")).thenReturn(folder);
        when(sysCollectionFolderService.save(folder)).thenAnswer(invocation -> {
            folder.setId(30L);
            return true;
        });
        when(contentModelMapper.toCollection(request, 7L, 30L, article)).thenReturn(collection);
        when(sysCollectionService.save(collection)).thenReturn(true);
        when(sysCollectionFolderService.updateById(folder)).thenReturn(true);
        when(blogArticleService.updateById(article)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCollectionService.createCollection(request);
        }

        assertEquals(Long.valueOf(30L), folder.getId());
        assertEquals(Integer.valueOf(1), folder.getCollectionCount());
        assertEquals(Integer.valueOf(3), article.getCollectCount());
        verify(contentModelMapper).toDefaultCollectionFolder(7L, "article");
        verify(sysCollectionFolderService).save(folder);
        verify(contentModelMapper).toCollection(request, 7L, 30L, article);
        verify(sysCollectionService).save(collection);
        verify(sysCollectionFolderService).updateById(folder);
        verify(blogArticleService).updateById(article);
    }

    @Test
    void deleteCollectionShouldRollbackFolderAndArticleCounts() {
        SysCollection collection = new SysCollection();
        collection.setId(1L);
        collection.setUserId(7L);
        collection.setFolderId(30L);
        collection.setTargetId(10L);
        collection.setTargetType("article");

        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(30L);
        folder.setCollectionCount(3);

        BlogArticle article = new BlogArticle();
        article.setId(10L);
        article.setCollectCount(4);

        when(sysCollectionService.getById(1L)).thenReturn(collection);
        when(sysCollectionFolderService.getById(30L)).thenReturn(folder);
        when(sysCollectionFolderService.updateById(folder)).thenReturn(true);
        when(blogArticleService.getById(10L)).thenReturn(article);
        when(blogArticleService.updateById(article)).thenReturn(true);
        when(sysCollectionService.removeById(1L)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCollectionService.deleteCollection(1L);
        }

        assertEquals(Integer.valueOf(2), folder.getCollectionCount());
        assertEquals(Integer.valueOf(3), article.getCollectCount());
        verify(sysCollectionFolderService).updateById(folder);
        verify(blogArticleService).updateById(article);
        verify(sysCollectionService).removeById(1L);
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
