package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.service.SysCollectionFolderService;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import com.cybzacg.blogbackend.module.content.service.impl.CollectionAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
class CollectionAdminServiceImplTest {
    @Mock
    private SysCollectionFolderService sysCollectionFolderService;
    @Mock
    private SysCollectionService sysCollectionService;
    @Mock
    private BlogArticleService blogArticleService;
    @Mock
    private ContentModelMapper contentModelMapper;

    private CollectionAdminServiceImpl collectionAdminService;

    @BeforeEach
    void setUp() {
        collectionAdminService = new CollectionAdminServiceImpl(
                sysCollectionFolderService,
                sysCollectionService,
                blogArticleService,
                contentModelMapper
        );
    }

    @Test
    void pageFoldersShouldReturnMappedRecords() {
        CollectionPageQuery query = new CollectionPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setUserId(7L);
        query.setTargetType("article");

        SysCollectionFolder folder = folder(11L, 7L, "article", 3);
        Page<SysCollectionFolder> page = new Page<>(2, 5, 1);
        page.setRecords(List.of(folder));

        CollectionFolderVO vo = new CollectionFolderVO();
        vo.setId(11L);
        vo.setUserId(7L);

        when(sysCollectionFolderService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(contentModelMapper.toCollectionFolderVO(folder)).thenReturn(vo);

        PageResult<CollectionFolderVO> result = collectionAdminService.pageFolders(query);

        assertEquals(1L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertSame(vo, result.getRecords().get(0));
    }

    @Test
    void pageCollectionsShouldReturnMappedRecords() {
        CollectionPageQuery query = new CollectionPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);
        query.setUserId(7L);
        query.setFolderId(30L);
        query.setTargetId(100L);
        query.setTargetType("article");

        SysCollection collection = collection(12L, 7L, 30L, 100L, "article");
        Page<SysCollection> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(collection));

        CollectionVO vo = new CollectionVO();
        vo.setId(12L);
        vo.setTargetId(100L);

        when(sysCollectionService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(contentModelMapper.toAdminCollectionVO(collection)).thenReturn(vo);

        PageResult<CollectionVO> result = collectionAdminService.pageCollections(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertSame(vo, result.getRecords().get(0));
    }

    @Test
    void deleteCollectionShouldRollbackFolderAndArticleCounts() {
        SysCollection collection = collection(15L, 7L, 30L, 100L, "article");
        SysCollectionFolder folder = folder(30L, 7L, "article", 3);
        BlogArticle article = article(100L, 4);

        when(sysCollectionService.getById(15L)).thenReturn(collection);
        when(sysCollectionFolderService.getById(30L)).thenReturn(folder);
        when(blogArticleService.getById(100L)).thenReturn(article);

        collectionAdminService.deleteCollection(15L);

        assertEquals(Integer.valueOf(2), folder.getCollectionCount());
        assertEquals(Integer.valueOf(3), article.getCollectCount());
        verify(sysCollectionFolderService).updateById(folder);
        verify(blogArticleService).updateById(article);
        verify(sysCollectionService).removeById(15L);
    }

    @Test
    void deleteCollectionShouldThrowWhenCollectionMissing() {
        when(sysCollectionService.getById(15L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> collectionAdminService.deleteCollection(15L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("收藏记录不存在", exception.getMessage());
        verify(sysCollectionFolderService, never()).updateById(any(SysCollectionFolder.class));
        verify(blogArticleService, never()).updateById(any(BlogArticle.class));
        verify(sysCollectionService, never()).removeById(15L);
    }

    private SysCollectionFolder folder(Long id, Long userId, String folderType, Integer collectionCount) {
        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(id);
        folder.setUserId(userId);
        folder.setFolderType(folderType);
        folder.setCollectionCount(collectionCount);
        return folder;
    }

    private SysCollection collection(Long id, Long userId, Long folderId, Long targetId, String targetType) {
        SysCollection collection = new SysCollection();
        collection.setId(id);
        collection.setUserId(userId);
        collection.setFolderId(folderId);
        collection.setTargetId(targetId);
        collection.setTargetType(targetType);
        return collection;
    }

    private BlogArticle article(Long id, Integer collectCount) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setCollectCount(collectCount);
        return article;
    }
}
