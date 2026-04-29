package com.cybzacg.blogbackend.module.content.collection;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.auth.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionFolderSaveRequest;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionVO;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionFolderRepository;
import com.cybzacg.blogbackend.module.content.collection.repository.SysCollectionRepository;
import com.cybzacg.blogbackend.module.content.collection.service.impl.UserCollectionServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCollectionServiceImplTest {
    @Mock
    private SysCollectionFolderRepository sysCollectionFolderRepository;
    @Mock
    private SysCollectionRepository sysCollectionRepository;
    @Mock
    private ArticleContentFacadeService articleContentFacadeService;
    @Mock
    private ContentModelMapper contentModelMapper;
    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    private UserCollectionServiceImpl userCollectionService;

    @BeforeEach
    void setUp() {
        userCollectionService = new UserCollectionServiceImpl(
                sysCollectionFolderRepository,
                sysCollectionRepository,
                articleContentFacadeService,
                contentModelMapper,
                notificationDeliveryService
        );
    }

    @Test
    void createCollectionShouldDelegateToFacadeService() {
        CollectionSaveRequest request = new CollectionSaveRequest();
        request.setTargetId(10L);
        request.setTargetType("article");

        SysCollectionFolder defaultFolder = new SysCollectionFolder();
        defaultFolder.setId(1L);
        defaultFolder.setUserId(7L);
        defaultFolder.setCollectionCount(0);
        when(sysCollectionFolderRepository.findDefaultByUserIdAndFolderType(7L, "article")).thenReturn(defaultFolder);

        BlogArticle article = new BlogArticle();
        article.setId(10L);
        article.setAuthorId(99L);
        when(articleContentFacadeService.requireInteractableArticle(10L, 7L, "收藏")).thenReturn(article);

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCollectionService.createCollection(request);
        }

        verify(articleContentFacadeService).requireInteractableArticle(10L, 7L, "收藏");
        verify(sysCollectionRepository).existsByUserIdAndFolderIdAndTargetIdAndTargetType(eq(7L), eq(1L), eq(10L), eq("article"));
    }

    @Test
    void deleteFolderShouldRollbackArticleCollectCounts() {
        SysCollectionFolder folder = new SysCollectionFolder();
        folder.setId(30L);
        folder.setUserId(7L);
        folder.setIsDefault(0);

        SysCollection collection1 = new SysCollection();
        collection1.setTargetId(10L);
        collection1.setTargetType("article");

        when(sysCollectionFolderRepository.getById(30L)).thenReturn(folder);
        when(sysCollectionRepository.findByFolderId(30L)).thenReturn(List.of(collection1));

        try (MockedStatic<?> securityUtils = SecurityTestUtils.mockUserId(7L)) {
            userCollectionService.deleteFolder(30L);
        }

        verify(articleContentFacadeService).adjustCollectCount(10L, -1);
        verify(sysCollectionRepository).removeByFolderId(30L);
        verify(sysCollectionFolderRepository).removeById(30L);
    }
}