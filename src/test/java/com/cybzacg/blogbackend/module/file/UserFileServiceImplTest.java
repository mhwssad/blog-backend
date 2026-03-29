package com.cybzacg.blogbackend.module.file;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileChunk;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitRequest;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileChunkService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.FileUploadTaskService;
import com.cybzacg.blogbackend.module.file.service.impl.UserFileServiceImpl;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private FileLifecycleService fileLifecycleService;
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
    private LambdaQueryChainWrapper<FileBusinessInfo> businessQuery;
    @Mock
    private LambdaQueryChainWrapper<FileChunk> chunkQuery;

    private UserFileServiceImpl userFileService;

    @BeforeEach
    void setUp() {
        userFileService = new UserFileServiceImpl(
                fileInfoService,
                fileUploadTaskService,
                fileChunkService,
                fileBusinessInfoService,
                fileLifecycleService,
                storageManager,
                fileUploadProperties
        );
    }

    @Test
    void initUploadTaskShouldRejectIllegalVisibility() {
        FileUploadInitRequest request = buildInitRequest();
        request.setIsPublic(2);
        when(fileUploadProperties.getAllowedExtensions()).thenReturn(List.of("png"));
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.initUploadTask(request, "127.0.0.1")
            );

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("文件可见性非法", exception.getMessage());
            verify(fileUploadTaskService, never()).save(any(FileUploadTask.class));
        }
    }

    @Test
    void initUploadTaskShouldRejectIncompleteChunkConfig() {
        FileUploadInitRequest request = buildInitRequest();
        request.setTotalChunks(3);
        when(fileUploadProperties.getAllowedExtensions()).thenReturn(List.of("png"));
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.initUploadTask(request, "127.0.0.1")
            );

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("分片参数不完整", exception.getMessage());
        }
    }

    @Test
    void initUploadTaskShouldRejectChunkTotalLessThanTwo() {
        FileUploadInitRequest request = buildInitRequest();
        request.setTotalChunks(1);
        request.setChunkSize(1024L);
        when(fileUploadProperties.getAllowedExtensions()).thenReturn(List.of("png"));
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.initUploadTask(request, "127.0.0.1")
            );

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("分片总数必须大于1", exception.getMessage());
        }
    }

    @Test
    void quickCheckShouldRejectExpiredTask() {
        FileUploadTask task = new FileUploadTask();
        task.setUploadId("upload-expired");
        task.setUploadUserId(7L);

        mockTaskLookup(task, true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.quickCheck("upload-expired", "127.0.0.1")
            );

            assertEquals(FileResultCode.UPLOAD_TASK_EXPIRED.getCode(), exception.getCode());
            verify(fileInfoService, never()).lambdaQuery();
            verify(storageManager, never()).getStorageService(anyString());
        }
    }

    @Test
    void uploadFileShouldDeleteObjectWhenFinalizeReferenceFails() {
        FileUploadTask task = buildFullUploadTask();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "hello world".getBytes());

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.upload(any(), anyString(), anyString())).thenReturn("https://cdn.example.com/f/33");
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null);
        when(fileInfoService.save(any(FileInfo.class))).thenAnswer(invocation -> {
            FileInfo fileInfo = invocation.getArgument(0);
            fileInfo.setId(33L);
            return true;
        });
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenThrow(new RuntimeException("save ref failed"));

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userFileService.uploadFile("upload-full", file, "127.0.0.1")
            );

            assertEquals("save ref failed", exception.getMessage());
            verify(storageService).upload(any(), anyString(), anyString());
            verify(storageService).delete(anyString());
            verify(fileLifecycleService, never()).refreshReferenceMetadata(any(Long.class), any(Boolean.class));
        }
    }

    @Test
    void uploadChunkShouldDeleteTempObjectWhenChunkPersistenceFails() {
        FileUploadTask task = new FileUploadTask();
        task.setId(8L);
        task.setUploadId("upload-chunk");
        task.setUploadUserId(7L);
        task.setStorageKey("local-test");
        task.setTotalChunks(2);
        task.setIsChunked(1);
        task.setTaskStatus(TaskStatusEnum.INIT.getValue());
        MockMultipartFile file = new MockMultipartFile("file", "chunk-1.part", "application/octet-stream", "chunk-data".getBytes());

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadProperties.getTempDirPrefix()).thenReturn("temp");
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.uploadToTemp(any(), anyString(), anyString())).thenReturn("temp/upload-chunk/chunk-1.part");
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.one()).thenReturn(null);
        when(fileChunkService.save(any(FileChunk.class))).thenThrow(new RuntimeException("save chunk failed"));

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userFileService.uploadChunk("upload-chunk", 1, file, null, "127.0.0.1")
            );

            assertEquals("save chunk failed", exception.getMessage());
            verify(storageService).uploadToTemp(any(), anyString(), anyString());
            verify(storageService).delete("temp/upload-chunk/chunk-1.part");
        }
    }

    @Test
    void completeUploadShouldDeleteMergedObjectWhenFinalizeReferenceFails() {
        FileUploadTask task = new FileUploadTask();
        task.setId(9L);
        task.setUploadId("upload-merge");
        task.setUploadUserId(7L);
        task.setStorageKey("local-test");
        task.setFileMd5("5eb63bbbe01eeed093cb22bb8f5acdc3");
        task.setFileSize(11L);
        task.setOriginalName("avatar.png");
        task.setMimeType("image/png");
        task.setReferenceType("avatar");
        task.setReferenceId(7L);
        task.setCategory("avatar");
        task.setIsPublic(1);
        task.setIsChunked(1);
        task.setTotalChunks(2);
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.count()).thenReturn(2L);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null);
        when(fileInfoService.save(any(FileInfo.class))).thenAnswer(invocation -> {
            FileInfo fileInfo = invocation.getArgument(0);
            fileInfo.setId(88L);
            return true;
        });
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenThrow(new RuntimeException("save ref failed"));
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.mergeFiles(any(), anyString())).thenReturn(true);
        when(storageService.getUrl(anyString())).thenReturn("https://cdn.example.com/f/88");

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userFileService.completeUpload("upload-merge", "127.0.0.1")
            );

            assertEquals("save ref failed", exception.getMessage());
            verify(storageService).mergeFiles(any(), anyString());
            verify(storageService).delete(anyString());
        }
    }

    @Test
    void deleteMyFileShouldDelegateLifecycleCleanup() {
        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(101L);
        reference.setUserId(7L);
        reference.setFileId(9L);
        when(fileBusinessInfoService.getById(101L)).thenReturn(reference);
        when(fileBusinessInfoService.removeById(101L)).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            userFileService.deleteMyFile(101L);

            verify(fileBusinessInfoService).removeById(101L);
            verify(fileLifecycleService).syncFileAfterReferenceRemoval(9L);
            verify(fileInfoService, never()).getById(any(Long.class));
            verify(storageManager, never()).getStorageService(anyString());
        }
    }

    private FileUploadInitRequest buildInitRequest() {
        FileUploadInitRequest request = new FileUploadInitRequest();
        request.setOriginalName("avatar.png");
        request.setFileSize(1024L);
        request.setFileMd5("abc123");
        request.setCategory("avatar");
        request.setReferenceType("avatar");
        return request;
    }

    private FileUploadTask buildFullUploadTask() {
        FileUploadTask task = new FileUploadTask();
        task.setId(6L);
        task.setUploadId("upload-full");
        task.setUploadUserId(7L);
        task.setStorageKey("local-test");
        task.setFileSize(11L);
        task.setOriginalName("avatar.png");
        task.setMimeType("image/png");
        task.setReferenceType("avatar");
        task.setReferenceId(7L);
        task.setCategory("avatar");
        task.setIsPublic(1);
        task.setIsChunked(0);
        task.setTaskStatus(TaskStatusEnum.INIT.getValue());
        return task;
    }

    private void mockTaskLookup(FileUploadTask task, boolean expired) {
        when(fileUploadTaskService.lambdaQuery()).thenReturn(taskQuery);
        when(taskQuery.eq(anySFunction(), any())).thenReturn(taskQuery);
        when(taskQuery.one()).thenReturn(task);
        when(fileLifecycleService.expireTaskIfNeeded(task)).thenReturn(expired);
    }

    private MockedStatic<SecurityUtils> mockUser() {
        MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::requireUserId).thenReturn(7L);
        return securityUtils;
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}
