package com.cybzacg.blogbackend.module.file;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileChunk;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.enums.storage.UploadModeEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.model.user.ChunkUploadVO;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitRequest;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitVO;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadResultVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFilePageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskPageQuery;
import com.cybzacg.blogbackend.module.file.model.user.UserFileTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
    void initUploadTaskShouldReturnFullUploadInitWhenNoExistingFile() {
        FileUploadInitRequest request = buildInitRequest();

        mockInitCommon();
        when(storageManager.getCurrentStorageKey()).thenReturn("local-test");
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null);
        when(fileUploadTaskService.save(any(FileUploadTask.class))).thenAnswer(invocation -> {
            FileUploadTask task = invocation.getArgument(0);
            task.setId(21L);
            return true;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadInitVO vo = userFileService.initUploadTask(request, "127.0.0.1");

            assertEquals(21L, vo.getTaskId());
            assertNotNull(vo.getUploadId());
            assertEquals(UploadModeEnum.FULL_UPLOAD.getValue(), vo.getUploadMode());
            assertTrue(vo.getQuickUploadAvailable());
            assertFalse(vo.getCompleted());
            assertNull(vo.getChunkSize());
            assertNull(vo.getTotalChunks());
            assertEquals(TaskStatusEnum.INIT.getValue(), vo.getTaskStatus());
            verify(fileUploadTaskService).save(any(FileUploadTask.class));
        }
    }

    @Test
    void initUploadTaskShouldReturnChunkedUploadInitWhenChunkConfigPresent() {
        FileUploadInitRequest request = buildInitRequest();
        request.setChunkSize(512L);
        request.setTotalChunks(3);

        mockInitCommon();
        when(storageManager.getCurrentStorageKey()).thenReturn("local-test");
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null);
        when(fileUploadTaskService.save(any(FileUploadTask.class))).thenAnswer(invocation -> {
            FileUploadTask task = invocation.getArgument(0);
            task.setId(22L);
            return true;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadInitVO vo = userFileService.initUploadTask(request, "127.0.0.1");

            assertEquals(22L, vo.getTaskId());
            assertNotNull(vo.getUploadId());
            assertEquals(UploadModeEnum.CHUNKED_UPLOAD.getValue(), vo.getUploadMode());
            assertFalse(vo.getCompleted());
            assertTrue(vo.getQuickUploadAvailable());
            assertEquals(512L, vo.getChunkSize());
            assertEquals(3, vo.getTotalChunks());
            assertEquals(TaskStatusEnum.INIT.getValue(), vo.getTaskStatus());
        }
    }

    @Test
    void initUploadTaskShouldQuickUploadWhenExistingFileMatches() {
        FileUploadInitRequest request = buildInitRequest();
        FileInfo existing = buildExistingFile(55L);

        mockInitCommon();
        when(storageManager.getCurrentStorageKey()).thenReturn("local-test");
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(existing);
        when(fileUploadTaskService.save(any(FileUploadTask.class))).thenAnswer(invocation -> {
            FileUploadTask task = invocation.getArgument(0);
            task.setId(23L);
            return true;
        });
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(91L);
            return true;
        });
        when(fileInfoService.getById(55L)).thenReturn(existing);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadInitVO vo = userFileService.initUploadTask(request, "127.0.0.1");

            assertEquals(UploadModeEnum.QUICK_UPLOAD.getValue(), vo.getUploadMode());
            assertTrue(vo.getQuickUploadAvailable());
            assertTrue(vo.getCompleted());
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), vo.getTaskStatus());
            assertEquals(55L, vo.getFileId());
            assertEquals(91L, vo.getBusinessId());
            assertEquals("https://cdn.example.com/f/55", vo.getFileUrl());
            verify(fileLifecycleService).refreshReferenceMetadata(55L, false);
        }
    }

    @Test
    void initUploadTaskShouldRejectIllegalVisibility() {
        FileUploadInitRequest request = buildInitRequest();
        request.setIsPublic(2);
        mockInitCommon();

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
    void initUploadTaskShouldRejectIllegalReferenceType() {
        FileUploadInitRequest request = buildInitRequest();
        request.setReferenceType("invalid");
        mockInitCommon();

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.initUploadTask(request, "127.0.0.1")
            );

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("文件引用类型非法", exception.getMessage());
            verify(fileUploadTaskService, never()).save(any(FileUploadTask.class));
        }
    }

    @Test
    void initUploadTaskShouldRejectIllegalCategory() {
        FileUploadInitRequest request = buildInitRequest();
        request.setCategory("invalid");
        mockInitCommon();

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.initUploadTask(request, "127.0.0.1")
            );

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("文件分类非法", exception.getMessage());
            verify(fileUploadTaskService, never()).save(any(FileUploadTask.class));
        }
    }
    @Test
    void initUploadTaskShouldRejectIncompleteChunkConfig() {
        FileUploadInitRequest request = buildInitRequest();
        request.setTotalChunks(3);
        mockInitCommon();

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
        mockInitCommon();

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
    void quickCheckShouldReturnQuickUploadWhenExistingFileMatches() {
        FileUploadTask task = buildFullUploadTask();
        task.setFileMd5("abc123");
        task.setFileSize(1024L);
        FileInfo existing = buildExistingFile(66L);
        FileBusinessInfo ref = new FileBusinessInfo();
        ref.setId(95L);

        mockTaskLookup(task, false);
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(existing);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(ref);
        when(fileInfoService.getById(66L)).thenReturn(existing);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO vo = userFileService.quickCheck("upload-full", "127.0.0.1");

            assertTrue(vo.getQuickUpload());
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), vo.getTaskStatus());
            assertEquals(66L, vo.getFileId());
            assertEquals(95L, vo.getBusinessId());
            assertEquals("https://cdn.example.com/f/66", vo.getFileUrl());
            verify(fileLifecycleService).refreshReferenceMetadata(66L, true);
        }
    }

    @Test
    void quickCheckShouldReturnPendingWhenNoExistingFile() {
        FileUploadTask task = buildFullUploadTask();
        task.setFileMd5("abc123");

        mockTaskLookup(task, false);
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO vo = userFileService.quickCheck("upload-full", "127.0.0.1");

            assertFalse(vo.getQuickUpload());
            assertEquals("upload-full", vo.getUploadId());
            assertEquals(6L, vo.getTaskId());
            assertEquals(TaskStatusEnum.INIT.getValue(), vo.getTaskStatus());
            assertNull(vo.getFileId());
            verify(fileUploadTaskService, never()).updateById(any(FileUploadTask.class));
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
    void pageMyFilesShouldReturnPagedFiles() {
        UserFilePageQuery query = new UserFilePageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setCategory("avatar");
        query.setReferenceType("avatar");

        FileBusinessInfo ref = new FileBusinessInfo();
        ref.setId(401L);
        ref.setFileId(501L);
        ref.setCategory("avatar");
        ref.setReferenceType("avatar");
        ref.setReferenceId(7L);
        ref.setIsPublic(1);
        Page<FileBusinessInfo> page = new Page<>(2L, 5L);
        page.setTotal(1L);
        page.setRecords(List.of(ref));

        FileInfo fileInfo = buildExistingFile(501L);
        fileInfo.setFileName("avatar-501.png");
        fileInfo.setOriginalName("avatar.png");
        fileInfo.setFileType("image");
        fileInfo.setMimeType("image/png");
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());

        when(fileBusinessInfoService.page(any(Page.class), any())).thenReturn(page);
        when(fileInfoService.listByIds(any())).thenReturn(List.of(fileInfo));

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            PageResult<UserFileVO> result = userFileService.pageMyFiles(query);

            assertEquals(1L, result.getTotal());
            assertEquals(2L, result.getCurrent());
            assertEquals(5L, result.getSize());
            assertEquals(1, result.getRecords().size());
            UserFileVO record = result.getRecords().get(0);
            assertEquals(401L, record.getBusinessId());
            assertEquals(501L, record.getFileId());
            assertEquals("avatar-501.png", record.getFileName());
            assertEquals("avatar", record.getCategory());
            assertEquals("avatar", record.getReferenceType());
            assertEquals(FileStatusEnum.NORMAL.getValue(), record.getStatus());
        }
    }
    @Test
    void pageMyUploadTasksShouldReturnPagedTasks() {
        UserFileTaskPageQuery query = new UserFileTaskPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        query.setIsChunked(1);

        FileUploadTask task = new FileUploadTask();
        task.setId(201L);
        task.setUploadId("upload-page");
        task.setFileId(301L);
        task.setOriginalName("avatar.png");
        task.setFileSize(2048L);
        task.setIsQuickUpload(0);
        task.setIsChunked(1);
        task.setChunkSize(1024L);
        task.setTotalChunks(2);
        task.setUploadedChunks(1);
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        Page<FileUploadTask> page = new Page<>(2L, 5L);
        page.setTotal(1L);
        page.setRecords(List.of(task));
        when(fileUploadTaskService.page(any(Page.class), any())).thenReturn(page);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            PageResult<UserFileTaskVO> result = userFileService.pageMyUploadTasks(query);

            assertEquals(1L, result.getTotal());
            assertEquals(2L, result.getCurrent());
            assertEquals(5L, result.getSize());
            assertEquals(1, result.getRecords().size());
            UserFileTaskVO record = result.getRecords().get(0);
            assertEquals(201L, record.getId());
            assertEquals("upload-page", record.getUploadId());
            assertEquals(301L, record.getFileId());
            assertEquals(TaskStatusEnum.UPLOADING.getValue(), record.getTaskStatus());
            assertEquals(1, record.getUploadedChunks());
            assertEquals(2, record.getTotalChunks());
        }
    }

    @Test
    void uploadFileShouldSucceedAndFinalizeReference() {
        FileUploadTask task = buildFullUploadTask();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "hello world".getBytes());
        FileInfo persistedFile = buildExistingFile(33L);
        persistedFile.setReferenceCount(1);

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
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(103L);
            return true;
        });
        when(fileInfoService.getById(33L)).thenReturn(persistedFile);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO result = userFileService.uploadFile("upload-full", file, "127.0.0.1");

            assertFalse(result.getQuickUpload());
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), result.getTaskStatus());
            assertEquals(33L, result.getFileId());
            assertEquals(103L, result.getBusinessId());
            assertEquals("https://cdn.example.com/f/33", result.getFileUrl());
            assertEquals(1, result.getReferenceCount());
            verify(fileLifecycleService).refreshReferenceMetadata(33L, true);
            verify(storageService, never()).delete(anyString());
        }
    }

    @Test
    void uploadFileShouldRejectMd5MismatchWhenValidationEnabled() {
        FileUploadTask task = buildFullUploadTask();
        task.setFileMd5("deadbeef");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "hello world".getBytes());

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(true);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.uploadFile("upload-full", file, "127.0.0.1")
            );

            assertEquals(FileResultCode.FILE_MD5_MISMATCH.getCode(), exception.getCode());
            verify(storageManager, never()).getStorageService(anyString());
        }
    }

    @Test
    void uploadFileShouldRejectTerminalTask() {
        FileUploadTask task = buildFullUploadTask();
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "hello world".getBytes());

        mockTaskLookup(task, false);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.uploadFile("upload-full", file, "127.0.0.1")
            );

            assertEquals(FileResultCode.UPLOAD_TASK_STATUS_INVALID.getCode(), exception.getCode());
            verify(storageManager, never()).getStorageService(anyString());
        }
    }

    @Test
    void uploadFileShouldReuseExistingFileWhenDuplicateKeyOccurs() {
        FileUploadTask task = buildFullUploadTask();
        task.setFileMd5("5eb63bbbe01eeed093cb22bb8f5acdc3");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "hello world".getBytes());
        FileInfo existing = buildExistingFile(77L);
        existing.setFileSize(11L);
        existing.setReferenceCount(3);

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.upload(any(), anyString(), anyString())).thenReturn("https://cdn.example.com/f/new-upload");
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null, existing);
        when(fileInfoService.save(any(FileInfo.class))).thenThrow(new DuplicateKeyException("duplicate md5"));
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(177L);
            return true;
        });
        when(fileInfoService.getById(77L)).thenReturn(existing);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO result = userFileService.uploadFile("upload-full", file, "127.0.0.1");

            assertTrue(result.getQuickUpload());
            assertEquals(77L, result.getFileId());
            assertEquals(177L, result.getBusinessId());
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), result.getTaskStatus());
            verify(storageService).delete(anyString());
        }
    }

    @Test
    void uploadFileShouldReviveDeletedFileWhenDuplicateKeyHitsDeletedRecord() {
        FileUploadTask task = buildFullUploadTask();
        task.setFileMd5("5eb63bbbe01eeed093cb22bb8f5acdc3");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "hello world".getBytes());
        FileInfo deletedFile = buildExistingFile(88L);
        deletedFile.setFileSize(11L);
        deletedFile.setStatus(FileStatusEnum.DELETED.getValue());
        deletedFile.setReferenceCount(0);

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.upload(any(), anyString(), anyString())).thenReturn("https://cdn.example.com/f/revive");
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null, deletedFile);
        when(fileInfoService.save(any(FileInfo.class))).thenThrow(new DuplicateKeyException("duplicate md5"));
        when(fileInfoService.updateById(deletedFile)).thenReturn(true);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(188L);
            return true;
        });
        when(fileInfoService.getById(88L)).thenReturn(deletedFile);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO result = userFileService.uploadFile("upload-full", file, "127.0.0.1");

            assertFalse(result.getQuickUpload());
            assertEquals(88L, result.getFileId());
            assertEquals(188L, result.getBusinessId());
            assertEquals(FileStatusEnum.NORMAL.getValue(), deletedFile.getStatus());
            verify(fileInfoService).updateById(deletedFile);
            verify(storageService, never()).delete(anyString());
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
    void uploadChunkShouldUploadChunkAndRefreshProgress() {
        FileUploadTask task = buildChunkTask();
        MockMultipartFile file = new MockMultipartFile("file", "chunk-1.part", "application/octet-stream", "chunk-data".getBytes());

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.uploadToTemp(any(), anyString(), anyString())).thenReturn("temp/upload-chunk/chunk-1.part");
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.one()).thenReturn(null);
        when(chunkQuery.count()).thenReturn(1L);
        when(fileChunkService.save(any(FileChunk.class))).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            ChunkUploadVO result = userFileService.uploadChunk("upload-chunk", 1, file, null, "127.0.0.1");

            assertEquals("upload-chunk", result.getUploadId());
            assertEquals(1, result.getChunkNumber());
            assertEquals(1, result.getUploadedChunks());
            assertEquals(2, result.getTotalChunks());
            assertEquals(TaskStatusEnum.UPLOADING.getValue(), result.getTaskStatus());
            verify(fileChunkService).save(any(FileChunk.class));
        }
    }

    @Test
    void uploadChunkShouldOverwriteExistingChunkRecord() {
        FileUploadTask task = buildChunkTask();
        FileChunk chunk = new FileChunk();
        chunk.setId(61L);
        chunk.setChunkMd5("old-md5");
        MockMultipartFile file = new MockMultipartFile("file", "chunk-1.part", "application/octet-stream", "chunk-data".getBytes());

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.uploadToTemp(any(), anyString(), anyString())).thenReturn("temp/upload-chunk/chunk-1.part");
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.one()).thenReturn(chunk);
        when(chunkQuery.count()).thenReturn(1L);
        when(fileChunkService.updateById(chunk)).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            ChunkUploadVO result = userFileService.uploadChunk("upload-chunk", 1, file, "ABCD1234", "127.0.0.1");

            assertEquals(1, result.getUploadedChunks());
            assertEquals(10L, chunk.getChunkSize());
            assertEquals("abcd1234", chunk.getChunkMd5());
            verify(fileChunkService).updateById(chunk);
            verify(fileChunkService, never()).save(any(FileChunk.class));
        }
    }

    @Test
    void uploadChunkShouldRejectChunkMd5MismatchWhenValidationEnabled() {
        FileUploadTask task = buildChunkTask();
        MockMultipartFile file = new MockMultipartFile("file", "chunk-1.part", "application/octet-stream", "chunk-data".getBytes());

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(true);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.uploadChunk("upload-chunk", 1, file, "deadbeef", "127.0.0.1")
            );

            assertEquals(FileResultCode.CHUNK_MD5_MISMATCH.getCode(), exception.getCode());
            verify(storageManager, never()).getStorageService(anyString());
        }
    }

    @Test
    void uploadChunkShouldRejectTerminalTask() {
        FileUploadTask task = buildChunkTask();
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        MockMultipartFile file = new MockMultipartFile("file", "chunk-1.part", "application/octet-stream", "chunk-data".getBytes());

        mockTaskLookup(task, false);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.uploadChunk("upload-chunk", 1, file, null, "127.0.0.1")
            );

            assertEquals(FileResultCode.UPLOAD_TASK_STATUS_INVALID.getCode(), exception.getCode());
            verify(storageManager, never()).getStorageService(anyString());
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
    void completeUploadShouldMergeAndCleanupChunkArtifactsWhenAllChunksUploaded() {
        FileUploadTask task = buildChunkTask();
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        task.setFileMd5("abc123");
        task.setFileSize(1024L);
        FileInfo persistedFile = buildExistingFile(99L);
        persistedFile.setReferenceCount(1);

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.count()).thenReturn(2L);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.mergeFiles(any(), anyString())).thenReturn(true);
        when(storageService.getUrl(anyString())).thenReturn("https://cdn.example.com/f/99");
        when(storageService.deleteTempFiles("upload-chunk")).thenReturn(true);
        when(fileChunkService.remove(any())).thenReturn(true);
        when(fileInfoService.save(any(FileInfo.class))).thenAnswer(invocation -> {
            FileInfo fileInfo = invocation.getArgument(0);
            fileInfo.setId(99L);
            return true;
        });
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(199L);
            return true;
        });
        when(fileInfoService.getById(99L)).thenReturn(persistedFile);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO result = userFileService.completeUpload("upload-chunk", "127.0.0.1");

            assertFalse(result.getQuickUpload());
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), result.getTaskStatus());
            assertEquals(99L, result.getFileId());
            assertEquals(199L, result.getBusinessId());
            verify(storageService).mergeFiles(any(), anyString());
            verify(fileChunkService).remove(any());
            verify(storageService).deleteTempFiles("upload-chunk");
        }
    }

    @Test
    void completeUploadShouldRejectWhenChunksIncomplete() {
        FileUploadTask task = buildChunkTask();
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());

        mockTaskLookup(task, false);
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.count()).thenReturn(1L);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.completeUpload("upload-chunk", "127.0.0.1")
            );

            assertEquals(FileResultCode.CHUNK_INCOMPLETE.getCode(), exception.getCode());
            verify(storageManager, never()).getStorageService(anyString());
        }
    }

    @Test
    void completeUploadShouldReuseExistingFileWhenMd5AlreadyExists() {
        FileUploadTask task = buildChunkTask();
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        task.setFileMd5("abc123");
        task.setFileSize(1024L);
        FileInfo existing = buildExistingFile(109L);
        existing.setReferenceCount(4);

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.count()).thenReturn(2L);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(existing);
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(209L);
            return true;
        });
        when(fileInfoService.getById(109L)).thenReturn(existing);
        when(fileChunkService.remove(any())).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.deleteTempFiles("upload-chunk")).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO result = userFileService.completeUpload("upload-chunk", "127.0.0.1");

            assertTrue(result.getQuickUpload());
            assertEquals(109L, result.getFileId());
            assertEquals(209L, result.getBusinessId());
            verify(storageService, never()).mergeFiles(any(), anyString());
            verify(fileChunkService).remove(any());
            verify(storageService).deleteTempFiles("upload-chunk");
        }
    }

    @Test
    void completeUploadShouldReuseExistingFileWhenDuplicateKeyOccursDuringPersist() {
        FileUploadTask task = buildChunkTask();
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        task.setFileMd5("abc123");
        task.setFileSize(1024L);
        FileInfo existing = buildExistingFile(119L);
        existing.setReferenceCount(3);

        mockTaskLookup(task, false);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadProperties.getTempDirPrefix()).thenReturn("temp");
        when(fileChunkService.lambdaQuery()).thenReturn(chunkQuery);
        when(chunkQuery.eq(anySFunction(), any())).thenReturn(chunkQuery);
        when(chunkQuery.count()).thenReturn(2L);
        when(fileUploadTaskService.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(fileInfoService.lambdaQuery()).thenReturn(fileInfoQuery);
        when(fileInfoQuery.eq(anySFunction(), any())).thenReturn(fileInfoQuery);
        when(fileInfoQuery.one()).thenReturn(null, existing);
        when(fileInfoService.save(any(FileInfo.class))).thenThrow(new DuplicateKeyException("duplicate md5"));
        when(fileBusinessInfoService.lambdaQuery()).thenReturn(businessQuery);
        when(businessQuery.eq(anySFunction(), any())).thenReturn(businessQuery);
        when(businessQuery.one()).thenReturn(null);
        when(fileBusinessInfoService.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(219L);
            return true;
        });
        when(fileInfoService.getById(119L)).thenReturn(existing);
        when(fileChunkService.remove(any())).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.mergeFiles(any(), anyString())).thenReturn(true);
        when(storageService.getUrl(anyString())).thenReturn("https://cdn.example.com/f/new-duplicate");
        when(storageService.deleteTempFiles("upload-chunk")).thenReturn(true);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            FileUploadResultVO result = userFileService.completeUpload("upload-chunk", "127.0.0.1");

            assertTrue(result.getQuickUpload());
            assertEquals(119L, result.getFileId());
            assertEquals(219L, result.getBusinessId());
            verify(storageService).mergeFiles(any(), anyString());
            verify(storageService).delete(anyString());
            verify(storageService).deleteTempFiles("upload-chunk");
        }
    }

    void completeUploadShouldRejectTerminalTask() {
        FileUploadTask task = buildChunkTask();
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());

        mockTaskLookup(task, false);

        try (MockedStatic<SecurityUtils> securityUtils = mockUser()) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> userFileService.completeUpload("upload-chunk", "127.0.0.1")
            );

            assertEquals(FileResultCode.UPLOAD_TASK_STATUS_INVALID.getCode(), exception.getCode());
            verify(fileChunkService, never()).lambdaQuery();
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

    private FileUploadTask buildChunkTask() {
        FileUploadTask task = new FileUploadTask();
        task.setId(8L);
        task.setUploadId("upload-chunk");
        task.setUploadUserId(7L);
        task.setStorageKey("local-test");
        task.setTotalChunks(2);
        task.setUploadedChunks(0);
        task.setIsChunked(1);
        task.setChunkSize(1024L);
        task.setTaskStatus(TaskStatusEnum.INIT.getValue());
        task.setOriginalName("avatar.png");
        task.setMimeType("image/png");
        task.setReferenceType("avatar");
        task.setReferenceId(7L);
        task.setCategory("avatar");
        task.setIsPublic(1);
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

    private void mockInitCommon() {
        when(fileUploadProperties.getAllowedExtensions()).thenReturn(List.of("png"));
        when(fileUploadProperties.getMaxFileSize()).thenReturn(10_240L);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        lenient().when(fileUploadProperties.getChunkSizeThreshold()).thenReturn(0L);
        lenient().when(fileUploadProperties.getTaskExpireDays()).thenReturn(2);
    }

    private FileInfo buildExistingFile(Long id) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(id);
        fileInfo.setMd5("abc123");
        fileInfo.setFileSize(1024L);
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        fileInfo.setFileUrl("https://cdn.example.com/f/" + id);
        fileInfo.setReferenceCount(2);
        return fileInfo;
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}














