package com.cybzacg.blogbackend.module.file;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadResultVO;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileChunkService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.module.file.service.FileUploadTaskService;
import com.cybzacg.blogbackend.module.file.service.impl.UserFileServiceImpl;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFileServiceImplTest {
    @Mock
    private FileInfoService fileInfoService;
    @Mock
    private FileUploadTaskService fileUploadTaskService;
    @Mock
    private FileChunkService fileChunkService;
    @Mock
    private FileBusinessInfoService fileBusinessInfoService;
    @Mock
    private StorageManager storageManager;
    @Mock
    private FileUploadProperties fileUploadProperties;
    @Mock
    private StorageService storageService;
    @Mock
    private LambdaQueryChainWrapper<FileUploadTask> taskQuery;
    @Mock
    private LambdaQueryChainWrapper<FileInfo> fileInfoQuery;
    @Mock
    private LambdaQueryChainWrapper<FileBusinessInfo> referenceQuery;
    @Mock
    private LambdaQueryChainWrapper<FileBusinessInfo> countQuery;

    private UserFileServiceImpl userFileService;

    @BeforeEach
    void setUp() {
        userFileService = new UserFileServiceImpl(
                fileInfoService,
                fileUploadTaskService,
                fileChunkService,
                fileBusinessInfoService,
                storageManager,
                fileUploadProperties
        );
    }

    @Test
    void quickCheckShouldReuseExistingFileAndMarkTaskCompleted() {
        FileUploadTask task = new FileUploadTask();
        task.setId(1L);
        task.setUploadId("upload-1");
        task.setUploadUserId(7L);
        task.setFileMd5("abc123");
        task.setFileSize(1024L);
        task.setReferenceType("article_attachment");
        task.setReferenceId(88L);
        task.setCategory("attachment");
        task.setIsPublic(1);
        task.setTaskStatus(TaskStatusEnum.INIT.getValue());

        FileInfo existingFile = new FileInfo();
        existingFile.setId(9L);
        existingFile.setStatus(FileStatusEnum.NORMAL.getValue());
        existingFile.setFileUrl("https://cdn.example.com/f/9");

        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(100L);
        reference.setFileId(9L);

        when(fileUploadTaskService.lambdaQuery()).thenReturn(taskQuery);
        when(taskQuery.eq(anySFunction(), any())).thenReturn(taskQuery);
        when(taskQuery.one()).thenReturn(task);

        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(existingFile);
        when(fileInfoService.updateById(existingFile)).thenReturn(true);

        when(fileBusinessInfoService.lambdaQuery()).thenReturn(referenceQuery, countQuery);
        when(referenceQuery.eq(anySFunction(), any())).thenReturn(referenceQuery);
        when(referenceQuery.one()).thenReturn(reference);
        when(countQuery.eq(anySFunction(), any())).thenReturn(countQuery);
        when(countQuery.count()).thenReturn(2L);
        when(fileUploadTaskService.updateById(task)).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(7L);

            FileUploadResultVO result = userFileService.quickCheck("upload-1", "127.0.0.1");

            assertTrue(Boolean.TRUE.equals(result.getQuickUpload()));
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), result.getTaskStatus());
            assertEquals(existingFile.getId(), result.getFileId());
            assertEquals(reference.getId(), result.getBusinessId());
            assertEquals(Integer.valueOf(2), existingFile.getReferenceCount());
            assertEquals(Integer.valueOf(1), task.getIsQuickUpload());
            assertEquals(existingFile.getId(), task.getReferencedFileId());
            assertEquals(existingFile.getId(), task.getFileId());
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), task.getTaskStatus());
            verify(fileInfoService).updateById(existingFile);
            verify(fileUploadTaskService).updateById(task);
        }
    }

    @Test
    void deleteMyFileShouldDeletePhysicalFileWhenLastReferenceRemoved() {
        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(101L);
        reference.setUserId(7L);
        reference.setFileId(9L);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(9L);
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        fileInfo.setStorageKey("local-test");
        fileInfo.setFilePath("attachment/2026/03/demo.png");

        when(fileBusinessInfoService.getById(101L)).thenReturn(reference);
        when(fileBusinessInfoService.removeById(101L)).thenReturn(true);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(countQuery);
        when(countQuery.eq(anySFunction(), any())).thenReturn(countQuery);
        when(countQuery.count()).thenReturn(0L);
        when(fileInfoService.getById(9L)).thenReturn(fileInfo);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(fileInfoService.updateById(fileInfo)).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(7L);

            userFileService.deleteMyFile(101L);

            verify(fileBusinessInfoService).removeById(101L);
            verify(storageService).delete("attachment/2026/03/demo.png");
            verify(fileInfoService).updateById(fileInfo);
            assertEquals(Integer.valueOf(0), fileInfo.getReferenceCount());
            assertEquals(FileStatusEnum.DELETED.getValue(), fileInfo.getStatus());
        }
    }

    @Test
    void deleteMyFileShouldOnlyDecreaseReferenceCountWhenReferencesRemain() {
        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(102L);
        reference.setUserId(7L);
        reference.setFileId(10L);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(10L);
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        fileInfo.setStorageKey("local-test");
        fileInfo.setFilePath("attachment/2026/03/demo-2.png");

        when(fileBusinessInfoService.getById(102L)).thenReturn(reference);
        when(fileBusinessInfoService.removeById(102L)).thenReturn(true);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(countQuery);
        when(countQuery.eq(anySFunction(), any())).thenReturn(countQuery);
        when(countQuery.count()).thenReturn(3L);
        when(fileInfoService.getById(10L)).thenReturn(fileInfo);
        when(fileInfoService.updateById(fileInfo)).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(7L);

            userFileService.deleteMyFile(102L);

            verify(fileBusinessInfoService).removeById(102L);
            verify(storageManager, never()).getStorageService(any());
            verify(fileInfoService).updateById(fileInfo);
            assertEquals(Integer.valueOf(3), fileInfo.getReferenceCount());
            assertEquals(FileStatusEnum.NORMAL.getValue(), fileInfo.getStatus());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
