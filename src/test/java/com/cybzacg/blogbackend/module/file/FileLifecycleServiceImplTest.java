package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.impl.FileLifecycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileLifecycleServiceImplTest {
    @Mock
    private FileInfoRepository fileInfoRepository;
    @Mock
    private FileUploadTaskRepository fileUploadTaskRepository;
    @Mock
    private FileChunkRepository fileChunkRepository;
    @Mock
    private FileBusinessInfoRepository fileBusinessInfoRepository;
    @Mock
    private StorageManager storageManager;
    @Mock
    private StorageService storageService;
    private FileLifecycleServiceImpl fileLifecycleService;

    @BeforeEach
    void setUp() {
        fileLifecycleService = new FileLifecycleServiceImpl(
                fileInfoRepository,
                fileUploadTaskRepository,
                fileChunkRepository,
                fileBusinessInfoRepository,
                storageManager);
    }

    @Test
    void refreshReferenceMetadataShouldIssueSubQueryUpdate() {
        when(fileInfoRepository.refreshReferenceMetadata(9L, true)).thenReturn(true);

        fileLifecycleService.refreshReferenceMetadata(9L, true);

        verify(fileInfoRepository).refreshReferenceMetadata(9L, true);
    }

    @Test
    void cleanupExpiredUploadTasksShouldCancelTaskAndCleanupArtifacts() {
        FileUploadTask task = buildExpiredChunkTask();
        when(fileUploadTaskRepository.findExpiredTasks(any(LocalDateTime.class), any(), any(Integer.class)))
                .thenReturn(List.of(task), List.of());
        when(fileUploadTaskRepository.updateById(task)).thenReturn(true);
        when(fileChunkRepository.deleteByUploadTaskId(task.getId())).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles("upload-expired")).thenReturn(true);

        int cleaned = fileLifecycleService.cleanupExpiredUploadTasks();

        assertEquals(1, cleaned);
        assertEquals(TaskStatusEnum.CANCELLED.getValue(), task.getTaskStatus());
        assertEquals(String.valueOf(FileResultCode.UPLOAD_TASK_EXPIRED.getCode()), task.getErrorCode());
        assertEquals(FileResultCode.UPLOAD_TASK_EXPIRED.getMessage(), task.getErrorMessage());
        assertNotNull(task.getCompleteTime());
        verify(fileUploadTaskRepository).updateById(task);
        verify(fileChunkRepository).deleteByUploadTaskId(task.getId());
        verify(storageService).deleteTempFiles("upload-expired");
    }

    @Test
    void cleanupExpiredUploadTasksShouldIgnoreStorageCleanupFailure() {
        FileUploadTask task = buildExpiredChunkTask();
        when(fileUploadTaskRepository.findExpiredTasks(any(LocalDateTime.class), any(), any(Integer.class)))
                .thenReturn(List.of(task), List.of());
        when(fileUploadTaskRepository.updateById(task)).thenReturn(true);
        when(fileChunkRepository.deleteByUploadTaskId(task.getId())).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles("upload-expired")).thenThrow(new RuntimeException("cleanup failed"));

        int cleaned = fileLifecycleService.cleanupExpiredUploadTasks();

        assertEquals(1, cleaned);
        assertEquals(TaskStatusEnum.CANCELLED.getValue(), task.getTaskStatus());
        verify(fileUploadTaskRepository).updateById(task);
        verify(fileChunkRepository).deleteByUploadTaskId(task.getId());
        verify(storageService).deleteTempFiles("upload-expired");
    }

    @Test
    void cleanupExpiredUploadTasksShouldContinueWhenBatchHitsLimit() {
        List<FileUploadTask> firstBatch = java.util.stream.IntStream.rangeClosed(1, 100)
                .mapToObj(id -> buildExpiredChunkTask(id))
                .toList();
        List<FileUploadTask> secondBatch = List.of(buildExpiredChunkTask(101));
        when(fileUploadTaskRepository.findExpiredTasks(any(LocalDateTime.class), any(), any(Integer.class)))
                .thenReturn(firstBatch, secondBatch);
        when(fileUploadTaskRepository.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(fileChunkRepository.deleteByUploadTaskId(any(Long.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles(any())).thenReturn(true);

        int cleaned = fileLifecycleService.cleanupExpiredUploadTasks();

        assertEquals(101, cleaned);
        verify(fileUploadTaskRepository, org.mockito.Mockito.times(2)).findExpiredTasks(any(LocalDateTime.class), any(),
                any(Integer.class));
        verify(fileUploadTaskRepository, org.mockito.Mockito.times(101)).updateById(any(FileUploadTask.class));
        verify(storageService, org.mockito.Mockito.times(101)).deleteTempFiles(any());
    }

    @Test
    void expireTaskIfNeededShouldReturnFalseForCompletedTask() {
        FileUploadTask task = new FileUploadTask();
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        task.setExpireTime(LocalDateTime.now().minusSeconds(1));

        boolean expired = fileLifecycleService.expireTaskIfNeeded(task);

        assertFalse(expired);
        verify(fileUploadTaskRepository, never()).updateById(any(FileUploadTask.class));
    }

    @Test
    void expireTaskIfNeededShouldCancelExpiredActiveTask() {
        FileUploadTask task = buildExpiredChunkTask();
        when(fileUploadTaskRepository.updateById(task)).thenReturn(true);
        when(fileChunkRepository.deleteByUploadTaskId(task.getId())).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles("upload-expired")).thenReturn(true);

        boolean expired = fileLifecycleService.expireTaskIfNeeded(task);

        assertTrue(expired);
        assertEquals(TaskStatusEnum.CANCELLED.getValue(), task.getTaskStatus());
        verify(fileUploadTaskRepository).updateById(task);
    }

    @Test
    void syncFileAfterReferenceRemovalShouldDeletePhysicalFileWhenReferencesReachZero() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(9L);
        fileInfo.setStorageKey("local-test");
        fileInfo.setFilePath("attachment/demo.png");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        when(fileInfoRepository.getById(9L)).thenReturn(fileInfo);
        when(fileBusinessInfoRepository.countByFileId(9L)).thenReturn(0L);
        when(fileInfoRepository.markDeletedIfNoReferences(9L)).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.delete("attachment/demo__chat_thumb.jpg")).thenReturn(true);
        when(storageService.delete("attachment/demo__chat_preview.wav")).thenReturn(true);
        when(storageService.delete("attachment/demo.png")).thenReturn(true);

        fileLifecycleService.syncFileAfterReferenceRemoval(9L);

        verify(fileInfoRepository).markDeletedIfNoReferences(9L);
        verify(storageService).delete("attachment/demo__chat_thumb.jpg");
        verify(storageService).delete("attachment/demo__chat_preview.wav");
        verify(storageService).delete("attachment/demo.png");
    }

    @Test
    void syncFileAfterReferenceRemovalShouldOnlyRefreshMetadataWhenReferencesRemain() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(10L);
        fileInfo.setStorageKey("local-test");
        fileInfo.setFilePath("attachment/demo-2.png");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        when(fileInfoRepository.getById(10L)).thenReturn(fileInfo);
        when(fileBusinessInfoRepository.countByFileId(10L)).thenReturn(2L);
        when(fileInfoRepository.refreshReferenceMetadata(10L, false)).thenReturn(true);

        fileLifecycleService.syncFileAfterReferenceRemoval(10L);

        verify(fileInfoRepository).refreshReferenceMetadata(10L, false);
        verify(storageManager, never()).getStorageService(anyString());
    }

    @Test
    void syncFileAfterReferenceRemovalShouldRefreshMetadataWhenConcurrentReferenceAppearsAfterZeroCountCheck() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(11L);
        fileInfo.setStorageKey("local-test");
        fileInfo.setFilePath("attachment/demo-3.png");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        when(fileInfoRepository.getById(11L)).thenReturn(fileInfo);
        when(fileBusinessInfoRepository.countByFileId(11L)).thenReturn(0L, 1L);
        when(fileInfoRepository.markDeletedIfNoReferences(11L)).thenReturn(false);
        when(fileInfoRepository.refreshReferenceMetadata(11L, false)).thenReturn(true);

        fileLifecycleService.syncFileAfterReferenceRemoval(11L);

        verify(fileInfoRepository).markDeletedIfNoReferences(11L);
        verify(fileInfoRepository).refreshReferenceMetadata(11L, false);
        verify(storageManager, never()).getStorageService(anyString());
    }

    private FileUploadTask buildExpiredChunkTask() {
        FileUploadTask task = new FileUploadTask();
        task.setId(1L);
        task.setUploadId("upload-expired");
        task.setStorageKey("local-test");
        task.setIsChunked(1);
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        task.setExpireTime(LocalDateTime.now().minusSeconds(1));
        return task;
    }

    private FileUploadTask buildExpiredChunkTask(long id) {
        FileUploadTask task = buildExpiredChunkTask();
        task.setId(id);
        task.setUploadId("upload-expired-" + id);
        return task;
    }
}
