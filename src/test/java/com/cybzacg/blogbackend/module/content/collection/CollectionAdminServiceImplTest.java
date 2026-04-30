package com.cybzacg.blogbackend.module.content.collection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.content.SysCollection;
import com.cybzacg.blogbackend.domain.content.SysCollectionFolder;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.content.collection.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.collection.model.admin.CollectionVO;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.collection.service.impl.CollectionAdminServiceImpl;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionAdminServiceImplTest {
    @Mock
    private SysCollectionFolderRepository sysCollectionFolderRepository;
    @Mock
    private SysCollectionRepository sysCollectionRepository;
    @Mock
    private ArticleContentFacadeService articleContentFacadeService;
    @Mock
    private ContentModelMapper contentModelMapper;

    private CollectionAdminServiceImpl collectionAdminService;

    @BeforeEach
    void setUp() {
        collectionAdminService = new CollectionAdminServiceImpl(
                sysCollectionFolderRepository,
                sysCollectionRepository,
                articleContentFacadeService,
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

        when(sysCollectionFolderRepository.pageByAdminConditions(query)).thenReturn(page);
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

        when(sysCollectionRepository.pageByAdminConditions(query)).thenReturn(page);
        when(contentModelMapper.toAdminCollectionVO(collection)).thenReturn(vo);

        PageResult<CollectionVO> result = collectionAdminService.pageCollections(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertSame(vo, result.getRecords().get(0));
    }

    @Test
    void deleteCollectionShouldDelegateToFacadeService() {
        SysCollection collection = collection(15L, 7L, 30L, 100L, "article");

        when(sysCollectionRepository.getById(15L)).thenReturn(collection);

        collectionAdminService.deleteCollection(15L);

        verify(articleContentFacadeService).adjustCollectCount(100L, -1);
        verify(sysCollectionRepository).removeById(15L);
    }

    @Test
    void deleteCollectionShouldThrowWhenCollectionMissing() {
        when(sysCollectionRepository.getById(15L)).thenReturn(null);

        assertThrows(com.cybzacg.blogbackend.exception.BusinessException.class,
            () -> collectionAdminService.deleteCollection(15L));

        verify(sysCollectionRepository, never()).removeById(15L);
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
}
