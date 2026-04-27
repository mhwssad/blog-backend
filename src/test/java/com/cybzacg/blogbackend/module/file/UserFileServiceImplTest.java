package com.cybzacg.blogbackend.module.file;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.enums.storage.UploadModeEnum;
import com.cybzacg.blogbackend.module.file.convert.FileModelMapper;
import com.cybzacg.blogbackend.module.file.model.user.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.impl.UserFileServiceImpl;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserFileServiceImplTest {
    @Mock
    private FileInfoRepository fileInfoRepository;
    @Mock
    private FileUploadTaskRepository fileUploadTaskRepository;
    @Mock
    private FileChunkRepository fileChunkRepository;
    @Mock
    private FileBusinessInfoRepository fileBusinessInfoRepository;
    @Mock
    private FileLifecycleService fileLifecycleService;
    @Mock
    private StorageManager storageManager;
    @Mock
    private FileUploadProperties fileUploadProperties;
    @Mock
    private FileModelMapper fileModelMapper;
    @Mock
    private StorageService storageService;

    private UserFileServiceImpl userFileService;

    @BeforeEach
    void setUp() {
        userFileService = new UserFileServiceImpl(
                fileInfoRepository,
                fileUploadTaskRepository,
                fileChunkRepository,
                fileBusinessInfoRepository,
                fileLifecycleService,
                storageManager,
                fileUploadProperties,
                fileModelMapper
        );
        when(fileUploadProperties.getAllowedExtensions()).thenReturn(List.of("png"));
        when(fileUploadProperties.getMaxFileSize()).thenReturn(10_240L);
        when(fileUploadProperties.getEnableMd5Check()).thenReturn(false);
        when(fileUploadProperties.getChunkSizeThreshold()).thenReturn(0L);
        when(fileUploadProperties.getTaskExpireDays()).thenReturn(2);
        when(fileUploadProperties.getTempDirPrefix()).thenReturn("temp");
        when(fileModelMapper.toFileUploadResultVO(any())).thenAnswer(invocation -> {
            FileUploadTask task = invocation.getArgument(0);
            FileUploadResultVO vo = new FileUploadResultVO();
            vo.setTaskId(task.getId());
            vo.setUploadId(task.getUploadId());
            vo.setTaskStatus(task.getTaskStatus());
            return vo;
        });
    }

    @Test
    void initUploadTaskShouldReturnFullUploadContextWhenNoExistingFile() {
        FileUploadInitRequest request = buildInitRequest();
        when(storageManager.getCurrentStorageKey()).thenReturn("local-test");
        when(fileInfoRepository.findByMd5AndStatus("abc123", FileStatusEnum.NORMAL.getValue())).thenReturn(null);
        when(fileUploadTaskRepository.save(any(FileUploadTask.class))).thenAnswer(invocation -> {
            FileUploadTask task = invocation.getArgument(0);
            task.setId(21L);
            return true;
        });

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            FileUploadInitVO result = userFileService.initUploadTask(request, "127.0.0.1");

            assertEquals(21L, result.getTaskId());
            assertEquals(UploadModeEnum.FULL_UPLOAD.getValue(), result.getUploadMode());
            assertTrue(result.getQuickUploadAvailable());
            assertFalse(result.getCompleted());
            verify(fileUploadTaskRepository).save(any(FileUploadTask.class));
        }
    }

    @Test
    void initUploadTaskShouldCompleteQuickUploadWhenExistingFileAvailable() {
        FileUploadInitRequest request = buildInitRequest();
        FileInfo existing = buildFileInfo(55L);
        existing.setReferenceCount(2);
        when(storageManager.getCurrentStorageKey()).thenReturn("local-test");
        when(fileInfoRepository.findByMd5AndStatus("abc123", FileStatusEnum.NORMAL.getValue())).thenReturn(existing);
        when(fileUploadTaskRepository.save(any(FileUploadTask.class))).thenAnswer(invocation -> {
            FileUploadTask task = invocation.getArgument(0);
            task.setId(22L);
            return true;
        });
        when(fileBusinessInfoRepository.findByFileUserReference(55L, 7L, "avatar", 7L)).thenReturn(null);
        when(fileBusinessInfoRepository.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(91L);
            return true;
        });
        when(fileInfoRepository.getById(55L)).thenReturn(existing);
        when(fileUploadTaskRepository.updateById(any(FileUploadTask.class))).thenReturn(true);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            FileUploadInitVO result = userFileService.initUploadTask(request, "127.0.0.1");

            assertTrue(result.getCompleted());
            assertEquals(UploadModeEnum.QUICK_UPLOAD.getValue(), result.getUploadMode());
            assertEquals(55L, result.getFileId());
            assertEquals(91L, result.getBusinessId());
            verify(fileLifecycleService).refreshReferenceMetadata(55L, false);
        }
    }

    @Test
    void pageMyFilesShouldAssemblePagedRecords() {
        UserFilePageQuery query = new UserFilePageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setCategory("avatar");

        FileBusinessInfo ref = new FileBusinessInfo();
        ref.setId(301L);
        ref.setFileId(401L);
        ref.setCategory("avatar");
        ref.setReferenceType("avatar");
        ref.setReferenceId(7L);
        ref.setIsPublic(1);

        Page<FileBusinessInfo> page = new Page<>(2L, 5L);
        page.setTotal(1L);
        page.setRecords(List.of(ref));

        FileInfo fileInfo = buildFileInfo(401L);
        fileInfo.setFileName("avatar-401.png");
        fileInfo.setOriginalName("avatar.png");

        when(fileBusinessInfoRepository.pageByUserAndFilters(eqUserId(), any(UserFilePageQuery.class), any())).thenReturn(page);
        when(fileInfoRepository.listByIds(any())).thenReturn(List.of(fileInfo));
        when(fileModelMapper.toUserFileVOFromBoth(any(), any())).thenAnswer(invocation -> {
            UserFileVO vo = new UserFileVO();
            vo.setBusinessId(invocation.getArgument(0, FileBusinessInfo.class).getId());
            vo.setFileId(invocation.getArgument(1, FileInfo.class).getId());
            return vo;
        });

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            PageResult<UserFileVO> result = userFileService.pageMyFiles(query);

            assertEquals(1L, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals(301L, result.getRecords().get(0).getBusinessId());
            assertEquals(401L, result.getRecords().get(0).getFileId());
        }
    }

    @Test
    void pageMyUploadTasksShouldReturnRepositoryPage() {
        UserFileTaskPageQuery query = new UserFileTaskPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        query.setIsChunked(1);

        FileUploadTask task = buildChunkTask();
        task.setId(501L);
        task.setFileId(601L);
        task.setUploadedChunks(1);
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());

        Page<FileUploadTask> page = new Page<>(2L, 5L);
        page.setTotal(1L);
        page.setRecords(List.of(task));
        when(fileUploadTaskRepository.pageByUserAndStatus(eqUserId(), any(UserFileTaskPageQuery.class))).thenReturn(page);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            PageResult<?> result = userFileService.pageMyUploadTasks(query);

            assertEquals(1L, result.getTotal());
            assertEquals(1, result.getRecords().size());
        }
    }

    @Test
    void uploadChunkShouldPersistChunkAndAdvanceProgress() {
        FileUploadTask task = buildChunkTask();
        MockMultipartFile file = new MockMultipartFile("file", "chunk-1.part", "application/octet-stream", "chunk-data".getBytes());

        mockTaskLookup(task);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.uploadToTemp(any(), anyString(), anyString())).thenReturn("temp/upload-chunk/chunk-1.part");
        when(fileChunkRepository.findByTaskIdAndChunkNumber(8L, 1)).thenReturn(null);
        when(fileChunkRepository.save(any())).thenReturn(true);
        when(fileChunkRepository.countByTaskIdAndStatus(8L, 2)).thenReturn(1L);
        when(fileUploadTaskRepository.updateById(any(FileUploadTask.class))).thenReturn(true);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            ChunkUploadVO result = userFileService.uploadChunk("upload-chunk", 1, file, null, "127.0.0.1");

            assertEquals(1, result.getUploadedChunks());
            assertEquals(2, result.getTotalChunks());
            verify(fileChunkRepository).save(any());
            verify(fileUploadTaskRepository, atLeast(2)).updateById(any(FileUploadTask.class));
        }
    }

    @Test
    void completeUploadShouldMergeAndFinalizeReference() {
        FileUploadTask task = buildChunkTask();
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        task.setFileMd5("abc123");
        task.setFileSize(1024L);
        FileInfo persistedFile = buildFileInfo(99L);
        persistedFile.setReferenceCount(1);

        mockTaskLookup(task);
        when(fileChunkRepository.countByTaskIdAndStatus(8L, 2)).thenReturn(2L);
        when(fileUploadTaskRepository.updateById(any(FileUploadTask.class))).thenReturn(true);
        when(fileInfoRepository.findByMd5AndStatus("abc123", FileStatusEnum.NORMAL.getValue())).thenReturn(null);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(storageService.mergeFiles(any(), anyString())).thenReturn(true);
        when(storageService.getUrl(anyString())).thenReturn("https://cdn.example.com/f/99");
        when(fileInfoRepository.save(any(FileInfo.class))).thenAnswer(invocation -> {
            FileInfo fileInfo = invocation.getArgument(0);
            fileInfo.setId(99L);
            return true;
        });
        when(fileBusinessInfoRepository.findByFileUserReference(99L, 7L, "avatar", 7L)).thenReturn(null);
        when(fileBusinessInfoRepository.save(any(FileBusinessInfo.class))).thenAnswer(invocation -> {
            FileBusinessInfo ref = invocation.getArgument(0);
            ref.setId(199L);
            return true;
        });
        when(fileInfoRepository.getById(99L)).thenReturn(persistedFile);
        when(fileChunkRepository.deleteByUploadTaskId(8L)).thenReturn(true);
        when(storageService.deleteTempFiles("upload-chunk")).thenReturn(true);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            FileUploadResultVO result = userFileService.completeUpload("upload-chunk", "127.0.0.1");

            assertEquals(99L, result.getFileId());
            assertEquals(199L, result.getBusinessId());
            assertEquals(TaskStatusEnum.COMPLETED.getValue(), result.getTaskStatus());
            verify(storageService).mergeFiles(any(), anyString());
            verify(fileChunkRepository).deleteByUploadTaskId(8L);
        }
    }

    @Test
    void deleteMyFileShouldRemoveReferenceAndDelegateLifecycle() {
        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(101L);
        reference.setUserId(7L);
        reference.setFileId(9L);
        when(fileBusinessInfoRepository.getById(101L)).thenReturn(reference);
        when(fileBusinessInfoRepository.removeById(101L)).thenReturn(true);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            userFileService.deleteMyFile(101L);

            verify(fileBusinessInfoRepository).removeById(101L);
            verify(fileLifecycleService).syncFileAfterReferenceRemoval(9L);
            verify(fileInfoRepository, never()).getById(anyLong());
        }
    }

    private void mockTaskLookup(FileUploadTask task) {
        when(fileUploadTaskRepository.findByUploadIdAndUserId(task.getUploadId(), 7L)).thenReturn(task);
        when(fileLifecycleService.expireTaskIfNeeded(task)).thenReturn(false);
    }

    private MockedStatic<SecurityUtils> mockUser() {
        MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::requireUserId).thenReturn(7L);
        return securityUtils;
    }

    private Long eqUserId() {
        return eq(7L);
    }

    private FileUploadInitRequest buildInitRequest() {
        FileUploadInitRequest request = new FileUploadInitRequest();
        request.setOriginalName("avatar.png");
        request.setFileSize(1024L);
        request.setFileMd5("abc123");
        request.setCategory("avatar");
        request.setReferenceType("avatar");
        request.setReferenceId(7L);
        return request;
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

    private FileInfo buildFileInfo(Long id) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(id);
        fileInfo.setMd5("abc123");
        fileInfo.setFileSize(1024L);
        fileInfo.setStatus(FileStatusEnum.NORMAL.getValue());
        fileInfo.setFileUrl("https://cdn.example.com/f/" + id);
        fileInfo.setReferenceCount(2);
        fileInfo.setFileName("demo.png");
        fileInfo.setOriginalName("demo.png");
        fileInfo.setMimeType("image/png");
        fileInfo.setFileType("image");
        return fileInfo;
    }
}
