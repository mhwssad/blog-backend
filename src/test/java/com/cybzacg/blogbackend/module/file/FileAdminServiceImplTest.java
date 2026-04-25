package com.cybzacg.blogbackend.module.file;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.enums.storage.TaskStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.model.admin.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileChunkRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileUploadTaskRepository;
import com.cybzacg.blogbackend.module.file.service.impl.FileAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileAdminServiceImplTest {
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
    private FileAdminServiceImpl fileAdminService;

    @BeforeEach
    void setUp() {
        fileAdminService = new FileAdminServiceImpl(
                fileInfoRepository,
                fileUploadTaskRepository,
                fileChunkRepository,
                fileBusinessInfoRepository,
                storageManager
        );
    }

    @Test
    void updateStatusShouldRejectDeletedStatusAndRequireDeleteApi() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileAdminService.updateStatus(1L, FileStatusEnum.DELETED.getValue())
        );

        assertEquals(FileResultCode.FILE_STATUS_INVALID.getCode(), exception.getCode());
        assertEquals("文件删除请使用删除接口，状态更新接口不支持设置为已删除", exception.getMessage());
        verify(fileInfoRepository, never()).getById(any());
        verify(fileInfoRepository, never()).updateById(any());
    }

    @Test
    void updateStatusShouldPersistNonDeletedStatus() {
        FileInfo file = new FileInfo();
        file.setId(1L);
        file.setStatus(FileStatusEnum.NORMAL.getValue());

        when(fileInfoRepository.getById(1L)).thenReturn(file);
        when(fileInfoRepository.updateById(file)).thenReturn(true);

        fileAdminService.updateStatus(1L, FileStatusEnum.BLOCKED.getValue());

        assertEquals(FileStatusEnum.BLOCKED.getValue(), file.getStatus());
        verify(fileInfoRepository).updateById(file);
    }

    @Test
    void updateStatusShouldRejectDeletedFileRecord() {
        FileInfo file = new FileInfo();
        file.setId(9L);
        file.setStatus(FileStatusEnum.DELETED.getValue());

        when(fileInfoRepository.getById(9L)).thenReturn(file);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileAdminService.updateStatus(9L, FileStatusEnum.NORMAL.getValue())
        );

        assertEquals(FileResultCode.FILE_STATUS_INVALID.getCode(), exception.getCode());
        verify(fileInfoRepository, never()).updateById(any());
    }

    @Test
    void pageFilesShouldReturnPagedRecords() {
        FileAdminPageQuery query = new FileAdminPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        query.setKeyword("demo");
        query.setUploadUserId(7L);
        query.setStatus(FileStatusEnum.NORMAL.getValue());
        query.setIsPublic(1);
        query.setCategory("attachment");
        query.setReferenceType("article_attachment");

        FileInfo file = buildAdminFile(1L);
        Page<FileInfo> page = new Page<>(2L, 5L);
        page.setTotal(1L);
        page.setRecords(List.of(file));
        when(fileInfoRepository.pageAdminFiles(any(FileAdminPageQuery.class))).thenReturn(page);

        PageResult<FileAdminVO> result = fileAdminService.pageFiles(query);

        assertEquals(1L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(1, result.getRecords().size());
        FileAdminVO record = result.getRecords().get(0);
        assertEquals(1L, record.getId());
        assertEquals("demo.png", record.getOriginalName());
        assertEquals("attachment", record.getCategory());
        assertEquals(FileStatusEnum.NORMAL.getValue(), record.getStatus());
    }

    @Test
    void pageFilesShouldRejectInvalidReferenceType() {
        FileAdminPageQuery query = new FileAdminPageQuery();
        query.setReferenceType("invalid");

        BusinessException exception = assertThrows(BusinessException.class, () -> fileAdminService.pageFiles(query));

        assertEquals(com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(fileInfoRepository, never()).pageAdminFiles(any());
    }

    @Test
    void getFileShouldAssembleFileDetail() {
        FileInfo file = buildAdminFile(9L);
        FileBusinessInfo reference = new FileBusinessInfo();
        reference.setId(101L);
        reference.setUserId(7L);
        reference.setReferenceType("article_attachment");
        reference.setReferenceId(88L);
        reference.setIsPublic(1);
        reference.setCategory("attachment");
        reference.setRemark("cover");

        FileUploadTask task = new FileUploadTask();
        task.setId(202L);
        task.setUploadId("upload-202");
        task.setFileId(9L);
        task.setUploadUserId(7L);
        task.setOriginalName("demo.png");
        task.setFileSize(2048L);
        task.setStorageKey("local-test");
        task.setIsQuickUpload(1);
        task.setIsChunked(0);
        task.setUploadedChunks(0);
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());

        when(fileInfoRepository.getById(9L)).thenReturn(file);
        when(fileBusinessInfoRepository.listByFileId(9L)).thenReturn(List.of(reference));
        when(fileUploadTaskRepository.listRecentByFileId(9L, 20)).thenReturn(List.of(task));

        FileDetailVO detail = fileAdminService.getFile(9L);

        assertEquals(9L, detail.getId());
        assertEquals("demo.png", detail.getOriginalName());
        assertEquals(1, detail.getReferences().size());
        assertEquals(1, detail.getTasks().size());
        assertEquals(101L, detail.getReferences().get(0).getId());
        assertEquals("article_attachment", detail.getReferences().get(0).getReferenceType());
        assertEquals(202L, detail.getTasks().get(0).getId());
        assertEquals("upload-202", detail.getTasks().get(0).getUploadId());
        assertEquals(TaskStatusEnum.COMPLETED.getValue(), detail.getTasks().get(0).getTaskStatus());
    }

    @Test
    void pageTasksShouldReturnPagedRecords() {
        FileTaskPageQuery query = new FileTaskPageQuery();
        query.setCurrent(3L);
        query.setSize(4L);
        query.setUploadUserId(7L);
        query.setTaskStatus(TaskStatusEnum.FAILED.getValue());
        query.setIsQuickUpload(0);
        query.setIsChunked(1);

        FileUploadTask task = new FileUploadTask();
        task.setId(303L);
        task.setUploadId("upload-303");
        task.setFileId(9L);
        task.setUploadUserId(7L);
        task.setOriginalName("chunk.zip");
        task.setFileSize(4096L);
        task.setStorageKey("local-test");
        task.setIsQuickUpload(0);
        task.setIsChunked(1);
        task.setUploadedChunks(2);
        task.setTotalChunks(4);
        task.setTaskStatus(TaskStatusEnum.FAILED.getValue());
        task.setErrorMessage("merge failed");
        Page<FileUploadTask> page = new Page<>(3L, 4L);
        page.setTotal(1L);
        page.setRecords(List.of(task));
        when(fileUploadTaskRepository.pageAdminTasks(any(FileTaskPageQuery.class))).thenReturn(page);

        PageResult<FileTaskAdminVO> result = fileAdminService.pageTasks(query);

        assertEquals(1L, result.getTotal());
        assertEquals(3L, result.getCurrent());
        assertEquals(4L, result.getSize());
        assertEquals(1, result.getRecords().size());
        FileTaskAdminVO record = result.getRecords().get(0);
        assertEquals(303L, record.getId());
        assertEquals("upload-303", record.getUploadId());
        assertEquals(TaskStatusEnum.FAILED.getValue(), record.getTaskStatus());
        assertEquals(2, record.getUploadedChunks());
        assertEquals(4, record.getTotalChunks());
    }

    @Test
    void pageTasksShouldRejectInvalidTaskStatus() {
        FileTaskPageQuery query = new FileTaskPageQuery();
        query.setTaskStatus(99);

        BusinessException exception = assertThrows(BusinessException.class, () -> fileAdminService.pageTasks(query));

        assertEquals(FileResultCode.UPLOAD_TASK_STATUS_INVALID.getCode(), exception.getCode());
        verify(fileUploadTaskRepository, never()).pageAdminTasks(any());
    }

    @Test
    void deleteFileShouldCleanupReferencesTasksChunksAndPhysicalFile() {
        FileInfo file = new FileInfo();
        file.setId(1L);
        file.setStorageKey("local-test");
        file.setFilePath("attachment/2026/03/demo.png");
        file.setStatus(FileStatusEnum.NORMAL.getValue());
        file.setReferenceCount(3);

        FileUploadTask task = new FileUploadTask();
        task.setId(11L);
        task.setUploadId("upload-11");
        task.setStorageKey("local-test");
        task.setFileId(1L);

        when(fileInfoRepository.getById(1L)).thenReturn(file);
        when(fileBusinessInfoRepository.deleteByFileId(1L)).thenReturn(true);
        when(fileUploadTaskRepository.listByFileId(1L)).thenReturn(List.of(task));
        when(fileChunkRepository.deleteByUploadTaskId(11L)).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(fileUploadTaskRepository.removeByIds(List.of(11L))).thenReturn(true);
        when(fileInfoRepository.updateById(file)).thenReturn(true);

        fileAdminService.deleteFile(1L);

        verify(fileBusinessInfoRepository).deleteByFileId(1L);
        verify(fileChunkRepository).deleteByUploadTaskId(11L);
        verify(storageService).deleteTempFiles("upload-11");
        verify(fileUploadTaskRepository).removeByIds(List.of(11L));
        verify(storageService).delete("attachment/2026/03/demo.png");
        verify(fileInfoRepository).updateById(file);
        assertEquals(Integer.valueOf(0), file.getReferenceCount());
        assertEquals(FileStatusEnum.DELETED.getValue(), file.getStatus());
    }

    @Test
    void deleteFileShouldNotRollbackMetadataWhenStorageCleanupFails() {
        FileInfo file = new FileInfo();
        file.setId(2L);
        file.setStorageKey("local-test");
        file.setFilePath("attachment/2026/03/demo-2.png");
        file.setStatus(FileStatusEnum.NORMAL.getValue());
        file.setReferenceCount(1);

        FileUploadTask task = new FileUploadTask();
        task.setId(22L);
        task.setUploadId("upload-22");
        task.setStorageKey("local-test");
        task.setFileId(2L);

        when(fileInfoRepository.getById(2L)).thenReturn(file);
        when(fileBusinessInfoRepository.deleteByFileId(2L)).thenReturn(true);
        when(fileUploadTaskRepository.listByFileId(2L)).thenReturn(List.of(task));
        when(fileChunkRepository.deleteByUploadTaskId(22L)).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(fileUploadTaskRepository.removeByIds(List.of(22L))).thenReturn(true);
        when(fileInfoRepository.updateById(file)).thenReturn(true);
        doThrow(new RuntimeException("temp cleanup failed")).when(storageService).deleteTempFiles("upload-22");
        doThrow(new RuntimeException("physical delete failed")).when(storageService).delete("attachment/2026/03/demo-2.png");

        assertDoesNotThrow(() -> fileAdminService.deleteFile(2L));

        verify(fileBusinessInfoRepository).deleteByFileId(2L);
        verify(fileChunkRepository).deleteByUploadTaskId(22L);
        verify(fileUploadTaskRepository).removeByIds(List.of(22L));
        verify(fileInfoRepository).updateById(file);
        assertEquals(Integer.valueOf(0), file.getReferenceCount());
        assertEquals(FileStatusEnum.DELETED.getValue(), file.getStatus());
    }

    private FileInfo buildAdminFile(Long id) {
        FileInfo file = new FileInfo();
        file.setId(id);
        file.setFileName("demo.png");
        file.setOriginalName("demo.png");
        file.setFilePath("attachment/2026/03/demo.png");
        file.setFileUrl("https://cdn.example.com/f/" + id);
        file.setStorageKey("local-test");
        file.setFileSize(2048L);
        file.setFileType("image");
        file.setMimeType("image/png");
        file.setFileExtension("png");
        file.setUploadUserId(7L);
        file.setIsPublic(1);
        file.setCategory("attachment");
        file.setStatus(FileStatusEnum.NORMAL.getValue());
        file.setReferenceCount(2);
        return file;
    }

}
