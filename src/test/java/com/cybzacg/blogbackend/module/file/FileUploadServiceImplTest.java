package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.domain.file.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileCategoryEnum;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.convert.FileModelConvert;
import com.cybzacg.blogbackend.module.file.convert.FileUploadConvert;
import com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.UserUploadInitRequest;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.impl.FileUploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceImplTest {
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
    private StorageService storageService;
    @Mock
    private FileModelConvert fileModelConvert;
    @Mock
    private FileUploadConvert fileUploadConvert;

    private FileUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        FileUploadProperties properties = new FileUploadProperties();
        properties.setTaskExpireDays(2);
        service = new FileUploadServiceImpl(
                fileInfoRepository,
                fileUploadTaskRepository,
                fileChunkRepository,
                fileBusinessInfoRepository,
                fileLifecycleService,
                storageManager,
                properties,
                fileModelConvert,
                fileUploadConvert
        );
    }

    @Test
    void initUploadTaskShouldUseQuickUploadWhenSameMd5Exists() {
        UserUploadInitRequest request = baseRequest();
        request.setFileMd5("d41d8cd98f00b204e9800998ecf8427e");
        request.setFileSize(100L);
        request.setCategory("attachment");
        request.setReferenceType("article_attachment");
        request.setReferenceId(1L);

        FileUploadTask task = new FileUploadTask();
        task.setId(1L);
        task.setUploadId("upload-1");
        task.setIsChunked(0);
        task.setTaskStatus(TaskStatusEnum.INIT.getValue());
        when(storageManager.getCurrentStorageKey()).thenReturn("local");
        when(fileUploadConvert.toFileUploadTask(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(task);
        when(fileInfoRepository.findByMd5AndStatus(anyString(), anyInt())).thenReturn(existingFile());
        when(fileUploadConvert.toFileBusinessInfo(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new FileBusinessInfo());
        when(fileBusinessInfoRepository.findByFileUserReference(any(), any(), any(), anyLong())).thenReturn(null);
        when(fileBusinessInfoRepository.save(any())).thenReturn(true);
        doNothing().when(fileLifecycleService).refreshReferenceMetadata(anyLong(), anyBoolean());
        when(fileInfoRepository.getById(anyLong())).thenAnswer(invocation -> existingFile());
        when(fileUploadTaskRepository.updateById(any())).thenReturn(true);
        when(fileModelConvert.toUserTaskVO(task)).thenReturn(new UserTaskVO());

        UserTaskVO result = service.initUploadTask(7L, request);

        assertTrue(result.getQuickUploadAvailable());
        assertTrue(result.getCompleted());
        assertEquals(1L, result.getFileId());
        verify(fileUploadTaskRepository).save(task);
    }

    @Test
    void uploadChunkShouldPersistChunkRecordAndReturnProgress() {
        FileUploadTask task = chunkTask();
        when(fileUploadTaskRepository.findByUploadIdAndUserId("upload-1", 7L)).thenReturn(task);
        when(storageManager.getStorageService("local")).thenReturn(storageService);
        when(fileUploadTaskRepository.updateById(task)).thenReturn(true);
        when(fileChunkRepository.findByTaskIdAndChunkNumber(1L, 2)).thenReturn(null);
        when(fileChunkRepository.countByTaskIdAndStatus(1L, 2)).thenReturn(1L);
        when(fileUploadConvert.toFileChunk(anyLong(), anyInt(), anyLong(), anyString())).thenAnswer(invocation -> {
            com.cybzacg.blogbackend.domain.file.FileChunk chunk = new com.cybzacg.blogbackend.domain.file.FileChunk();
            chunk.setUploadTaskId(invocation.getArgument(0));
            chunk.setChunkNumber(invocation.getArgument(1));
            chunk.setChunkSize(invocation.getArgument(2));
            chunk.setChunkMd5(invocation.getArgument(3));
            return chunk;
        });
        when(fileModelConvert.toUserTaskVO(task)).thenReturn(new UserTaskVO());

        UserTaskVO result = service.uploadChunk(7L, "upload-1", 2, fileInfo("chunk.txt"), inputStream("abc"));

        assertEquals(Integer.valueOf(2), result.getChunkNumber());
        verify(storageService).uploadToTemp(any(), contains("chunk-2.part"), any());
        verify(fileChunkRepository).save(any());
    }

    @Test
    void completeUploadShouldRejectWhenChunksIncomplete() {
        FileUploadTask task = chunkTask();
        when(fileUploadTaskRepository.findByUploadIdAndUserId("upload-1", 7L)).thenReturn(task);
        when(fileChunkRepository.countByTaskIdAndStatus(1L, 2)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.completeUpload(7L, "upload-1"));

        assertEquals(FileResultCode.CHUNK_INCOMPLETE.getCode(), exception.getCode());
    }

    private UserUploadInitRequest baseRequest() {
        UserUploadInitRequest request = new UserUploadInitRequest();
        request.setOriginalName("demo.png");
        request.setFileSize(100L);
        request.setCategory("attachment");
        request.setReferenceType("article_attachment");
        request.setReferenceId(1L);
        request.setIsPublic(1);
        return request;
    }

    private FileInfo existingFile() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(1L);
        fileInfo.setFileUrl("https://cdn.example.com/1");
        fileInfo.setFileSize(100L);
        fileInfo.setStatus(1);
        return fileInfo;
    }

    private FileUploadTask chunkTask() {
        FileUploadTask task = new FileUploadTask();
        task.setId(1L);
        task.setUploadId("upload-1");
        task.setUploadUserId(7L);
        task.setStorageKey("local");
        task.setOriginalName("demo.txt");
        task.setCategory(FileCategoryEnum.ATTACHMENT.getValue());
        task.setTaskStatus(TaskStatusEnum.UPLOADING.getValue());
        task.setIsChunked(1);
        task.setTotalChunks(3);
        task.setFileSize(100L);
        task.setIsPublic(1);
        return task;
    }

    private FileInfo fileInfo(String name) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(name);
        fileInfo.setMimeType("text/plain");
        fileInfo.setFileSize(3L);
        fileInfo.setMd5("abc");
        return fileInfo;
    }

    private InputStream inputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}
