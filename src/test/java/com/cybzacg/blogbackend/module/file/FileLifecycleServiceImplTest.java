package com.cybzacg.blogbackend.module.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileChunk;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileChunkService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.module.file.service.FileUploadTaskService;
import com.cybzacg.blogbackend.module.file.service.impl.FileLifecycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileLifecycleServiceImplTest {
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
    private StorageService storageService;
    @Mock
    private LambdaQueryChainWrapper<FileUploadTask> taskQuery;
    @Mock
    private LambdaQueryChainWrapper<FileBusinessInfo> businessQuery;

    private FileLifecycleServiceImpl fileLifecycleService;

    @BeforeEach
    void setUp() {
        fileLifecycleService = new FileLifecycleServiceImpl(
                fileInfoService,
                fileUploadTaskService,
                fileChunkService,
                fileBusinessInfoService,
                storageManager
        );
    }

    @Test
    void refreshReferenceMetadataShouldIssueSubQueryUpdate() {
        when(fileInfoService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        fileLifecycleService.refreshReferenceMetadata(9L, true);

        verify(fileInfoService).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    void cleanupExpiredUploadTasksShouldCancelTaskAndCleanupArtifacts() {
        FileUploadTask task = buildExpiredChunkTask();
        when(fileUploadTaskService.lambdaQuery()).thenReturn(taskQuery);
        when(taskQuery.le(anySFunction(), any())).thenReturn(taskQuery);
        when(taskQuery.in(anySFunction(), anyCollection())).thenReturn(taskQuery);
        when(taskQuery.orderByAsc(anySFunction())).thenReturn(taskQuery);
        when(taskQuery.last(anyString())).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task), List.of());
        when(fileUploadTaskService.updateById(task)).thenReturn(true);
        when(fileChunkService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles("upload-expired")).thenReturn(true);

        int cleaned = fileLifecycleService.cleanupExpiredUploadTasks();

        assertEquals(1, cleaned);
        assertEquals(TaskStatusEnum.CANCELLED.getValue(), task.getTaskStatus());
        assertEquals(String.valueOf(FileResultCode.UPLOAD_TASK_EXPIRED.getCode()), task.getErrorCode());
        assertEquals(FileResultCode.UPLOAD_TASK_EXPIRED.getMessage(), task.getErrorMessage());
        assertNotNull(task.getCompleteTime());
        verify(fileUploadTaskService).updateById(task);
        verify(fileChunkService).remove(any(LambdaQueryWrapper.class));
        verify(storageService).deleteTempFiles("upload-expired");
    }

    @Test
    void cleanupExpiredUploadTasksShouldIgnoreStorageCleanupFailure() {
        FileUploadTask task = buildExpiredChunkTask();
        when(fileUploadTaskService.lambdaQuery()).thenReturn(taskQuery);
        when(taskQuery.le(anySFunction(), any())).thenReturn(taskQuery);
        when(taskQuery.in(anySFunction(), anyCollection())).thenReturn(taskQuery);
        when(taskQuery.orderByAsc(anySFunction())).thenReturn(taskQuery);
        when(taskQuery.last(anyString())).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task), List.of());
        when(fileUploadTaskService.updateById(task)).thenReturn(true);
        when(fileChunkService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles("upload-expired")).thenThrow(new RuntimeException("cleanup failed"));

        int cleaned = fileLifecycleService.cleanupExpiredUploadTasks();

        assertEquals(1, cleaned);
        assertEquals(TaskStatusEnum.CANCELLED.getValue(), task.getTaskStatus());
        verify(fileUploadTaskService).updateById(task);
        verify(fileChunkService).remove(any(LambdaQueryWrapper.class));
        verify(storageService).deleteTempFiles("upload-expired");
    }

    @Test
    void expireTaskIfNeededShouldReturnFalseForCompletedTask() {
        FileUploadTask task = new FileUploadTask();
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        task.setExpireTime(new Date(System.currentTimeMillis() - 1000L));

        boolean expired = fileLifecycleService.expireTaskIfNeeded(task);

        assertFalse(expired);
        verify(fileUploadTaskService, never()).updateById(any(FileUploadTask.class));
    }

    @Test
    void expireTaskIfNeededShouldCancelExpiredActiveTask() {
        FileUploadTask task = buildExpiredChunkTask();
        when(fileUploadTaskService.updateById(task)).thenReturn(true);
        when(fileChunkService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles("upload-expired")).thenReturn(true);

        boolean expired = fileLifecycleService.expireTaskIfNeeded(task);

        assertTrue(expired);
        assertEquals(TaskStatusEnum.CANCELLED.getValue(), task.getTaskStatus());
        verify(fileUploadTaskService).updateById(task);
    }

    @Test
    void syncFileAfterReferenceRemovalShouldDeletePhysicalFileWhenReferencesReachZero() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(9L);
        fileInfo.setStorageKey("local-test");
        fileInfo.setFilePath("attachment/demo.png");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        when(fileInfoService.getById(9L)).thenReturn(fileInfo);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.count()).thenReturn(0L);
        when(fileInfoService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.delete("attachment/demo.png")).thenReturn(true);

        fileLifecycleService.syncFileAfterReferenceRemoval(9L);

        verify(fileInfoService).update(any(LambdaUpdateWrapper.class));
        verify(storageService).delete("attachment/demo.png");
    }

    @Test
    void syncFileAfterReferenceRemovalShouldOnlyRefreshMetadataWhenReferencesRemain() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(10L);
        fileInfo.setStorageKey("local-test");
        fileInfo.setFilePath("attachment/demo-2.png");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        when(fileInfoService.getById(10L)).thenReturn(fileInfo);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.count()).thenReturn(2L);
        when(fileInfoService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        fileLifecycleService.syncFileAfterReferenceRemoval(10L);

        verify(fileInfoService).update(any(LambdaUpdateWrapper.class));
        verify(storageManager, never()).getStorageService(anyString());
    }

    private FileUploadTask buildExpiredChunkTask() {
        FileUploadTask task = new FileUploadTask();
        task.setId(1L);
        task.setUploadId("upload-expired");
        task.setStorageKey("local-test");
        task.setIsChunked(1);
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        task.setExpireTime(new Date(System.currentTimeMillis() - 1000L));
        return task;
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
