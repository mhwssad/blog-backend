package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.ArticleAccessControlService;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionVO;
import com.cybzacg.blogbackend.module.content.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.repository.SysCollectionRepository;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCollectionServiceImplTest {
    @Mock
    private SysCollectionFolderRepository sysCollectionFolderRepository;
    @Mock
    private SysCollectionRepository sysCollectionRepository;
    @Mock
    private BlogArticleRepository blogArticleService;
    @Mock
    private ArticleAccessControlService articleAccessControlService;
    @Mock
    private ContentModelMapper contentModelMapper;

    private UserCollectionServiceImpl userCollectionService;

    @BeforeEach
    void setUp() {
        userCollectionService = new UserCollectionServiceImpl(
                sysCollectionFolderRepository,
                sysCollectionRepository,
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
        when(sysCollectionFolderRepository.findDefaultByUserIdAndFolderType(7L, "article")).thenReturn(null);
        when(sysCollectionFolderRepository.findDefaultsByUserIdAndFolderType(7L, "article")).thenReturn(List.of());
        when(sysCollectionRepository.existsByUserIdAndFolderIdAndTargetIdAndTargetType(7L, 30L, 10L, "article")).thenReturn(false);
        when(contentModelMapper.toDefaultCollectionFolder(7L, "article")).thenReturn(folder);
        when(sysCollectionFolderRepository.save(folder)).thenAnswer(invocation -> {
            folder.setId(30L);
            return true;
        });
        when(contentModelMapper.toCollection(request, 7L, 30L, article)).thenReturn(collection);
        when(sysCollectionRepository.save(collection)).thenReturn(true);
        when(sysCollectionFolderRepository.updateById(folder)).thenReturn(true);
        when(blogArticleService.updateById(article)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCollectionService.createCollection(request);
        }

        assertEquals(Long.valueOf(30L), folder.getId());
        assertEquals(Integer.valueOf(1), folder.getCollectionCount());
        assertEquals(Integer.valueOf(3), article.getCollectCount());
        verify(contentModelMapper).toDefaultCollectionFolder(7L, "article");
        verify(sysCollectionFolderRepository).save(folder);
        verify(contentModelMapper).toCollection(request, 7L, 30L, article);
        verify(sysCollectionRepository).save(collection);
        verify(sysCollectionFolderRepository).updateById(folder);
        verify(blogArticleService).updateById(article);
    }

    @Test
    void createFolderShouldUnsetExistingDefaultAndReturnMappedFolder() {
        CollectionFolderSaveRequest request = new CollectionFolderSaveRequest();
        request.setFolderName("Read Later");
        request.setIsDefault(1);

        SysCollectionFolder existingDefault = new SysCollectionFolder();
        existingDefault.setId(9L);
        existingDefault.setUserId(7L);
        existingDefault.setFolderType("article");
        existingDefault.setIsDefault(1);

        SysCollectionFolder folder = new SysCollectionFolder();
        CollectionFolderVO vo = new CollectionFolderVO();
        vo.setId(30L);

        when(contentModelMapper.toCollectionFolder(request)).thenReturn(folder);
        when(sysCollectionFolderRepository.findDefaultsByUserIdAndFolderType(7L, "article")).thenReturn(List.of(existingDefault));
        when(sysCollectionFolderRepository.save(folder)).thenAnswer(invocation -> {
            folder.setId(30L);
            return true;
        });
        when(contentModelMapper.toCollectionFolderVO(folder)).thenReturn(vo);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            CollectionFolderVO result = userCollectionService.createFolder(request);

            assertSame(vo, result);
        }

        assertEquals(Integer.valueOf(0), existingDefault.getIsDefault());
        assertEquals(Long.valueOf(7L), folder.getUserId());
        assertEquals("article", folder.getFolderType());
        assertEquals(Integer.valueOf(0), folder.getIsPublic());
        assertEquals(Integer.valueOf(1), folder.getIsDefault());
        assertEquals(Integer.valueOf(0), folder.getSortOrder());
        assertEquals(Integer.valueOf(0), folder.getCollectionCount());
        verify(sysCollectionFolderRepository).updateById(existingDefault);
        verify(sysCollectionFolderRepository).save(folder);
    }

    @Test
    void updateFolderShouldUnsetOtherDefaultFolderAndReturnMappedFolder() {
        CollectionFolderSaveRequest request = new CollectionFolderSaveRequest();
        request.setFolderName("Starred");
        request.setIsDefault(1);
        request.setIsPublic(1);
        request.setSortOrder(8);

        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(30L);
        folder.setUserId(7L);
        folder.setFolderType("article");
        folder.setCollectionCount(2);

        SysCollectionFolder otherDefault = new SysCollectionFolder();
        otherDefault.setId(31L);
        otherDefault.setUserId(7L);
        otherDefault.setFolderType("article");
        otherDefault.setIsDefault(1);

        CollectionFolderVO vo = new CollectionFolderVO();
        vo.setId(30L);

        when(sysCollectionFolderRepository.getById(30L)).thenReturn(folder);
        when(sysCollectionFolderRepository.findDefaultsByUserIdAndFolderType(7L, "article")).thenReturn(List.of(folder, otherDefault));
        when(contentModelMapper.toCollectionFolderVO(folder)).thenReturn(vo);
        org.mockito.Mockito.doAnswer(invocation -> {
            CollectionFolderSaveRequest actualRequest = invocation.getArgument(0);
            SysCollectionFolder actualFolder = invocation.getArgument(1);
            actualFolder.setFolderName(actualRequest.getFolderName());
            return null;
        }).when(contentModelMapper).updateCollectionFolder(request, folder);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            CollectionFolderVO result = userCollectionService.updateFolder(30L, request);

            assertSame(vo, result);
        }

        assertEquals("Starred", folder.getFolderName());
        assertEquals(Integer.valueOf(1), folder.getIsDefault());
        assertEquals(Integer.valueOf(1), folder.getIsPublic());
        assertEquals(Integer.valueOf(8), folder.getSortOrder());
        assertEquals(Integer.valueOf(0), otherDefault.getIsDefault());
        verify(sysCollectionFolderRepository).updateById(otherDefault);
        verify(sysCollectionFolderRepository).updateById(folder);
    }

    @Test
    void deleteFolderShouldThrowWhenFolderIsDefault() {
        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(30L);
        folder.setUserId(7L);
        folder.setIsDefault(1);

        when(sysCollectionFolderRepository.getById(30L)).thenReturn(folder);

        BusinessException exception;
        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            exception = assertThrows(BusinessException.class, () -> userCollectionService.deleteFolder(30L));
        }

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("默认收藏夹不可删除", exception.getMessage());
        verify(sysCollectionRepository, never()).removeByFolderId(30L);
        verify(sysCollectionFolderRepository, never()).removeById(30L);
    }

    @Test
    void deleteFolderShouldRollbackArticleCountsAndRemoveFolderCollections() {
        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(30L);
        folder.setUserId(7L);
        folder.setIsDefault(0);

        SysCollection collection = new SysCollection();
        collection.setId(1L);
        collection.setFolderId(30L);
        collection.setTargetId(10L);
        collection.setTargetType("article");

        BlogArticle article = new BlogArticle();
        article.setId(10L);
        article.setCollectCount(4);

        when(sysCollectionFolderRepository.getById(30L)).thenReturn(folder);
        when(sysCollectionRepository.findByFolderId(30L)).thenReturn(List.of(collection));
        when(blogArticleService.getById(10L)).thenReturn(article);
        when(blogArticleService.updateById(article)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCollectionService.deleteFolder(30L);
        }

        assertEquals(Integer.valueOf(3), article.getCollectCount());
        verify(sysCollectionRepository).removeByFolderId(30L);
        verify(sysCollectionFolderRepository).removeById(30L);
    }

    @Test
    void pageCollectionsShouldReturnMappedRecords() {
        SysCollection collection = new SysCollection();
        collection.setId(1L);
        collection.setUserId(7L);

        CollectionVO vo = new CollectionVO();
        vo.setId(1L);

        Page<SysCollection> page = new Page<>(1, 100, 1);
        page.setRecords(List.of(collection));

        when(sysCollectionRepository.pageByUserId(7L, 1, 100)).thenReturn(page);
        when(contentModelMapper.toUserCollectionVO(collection)).thenReturn(vo);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            PageResult<CollectionVO> result = userCollectionService.pageCollections();

            assertEquals(1L, result.getTotal());
            assertSame(vo, result.getRecords().get(0));
        }
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

        when(sysCollectionRepository.getById(1L)).thenReturn(collection);
        when(sysCollectionFolderRepository.getById(30L)).thenReturn(folder);
        when(sysCollectionFolderRepository.updateById(folder)).thenReturn(true);
        when(blogArticleService.getById(10L)).thenReturn(article);
        when(blogArticleService.updateById(article)).thenReturn(true);
        when(sysCollectionRepository.removeById(1L)).thenReturn(true);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCollectionService.deleteCollection(1L);
        }

        assertEquals(Integer.valueOf(2), folder.getCollectionCount());
        assertEquals(Integer.valueOf(3), article.getCollectCount());
        verify(sysCollectionFolderRepository).updateById(folder);
        verify(blogArticleService).updateById(article);
        verify(sysCollectionRepository).removeById(1L);
    }
}
