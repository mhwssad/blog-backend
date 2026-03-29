package com.cybzacg.blogbackend.module.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.domain.FileUploadTask;
import com.cybzacg.blogbackend.enums.file.FileResultCode;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.service.FileBusinessInfoService;
import com.cybzacg.blogbackend.module.file.service.FileChunkService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.module.file.service.FileUploadTaskService;
import com.cybzacg.blogbackend.module.file.service.impl.FileAdminServiceImpl;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminPageQuery;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskPageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAdminServiceImplTest {
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

    private FileAdminServiceImpl fileAdminService;

    @BeforeEach
    void setUp() {
        fileAdminService = new FileAdminServiceImpl(
                fileInfoService,
                fileUploadTaskService,
                fileChunkService,
                fileBusinessInfoService,
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
        verify(fileInfoService, never()).getById(any());
        verify(fileInfoService, never()).updateById(any());
    }

    @Test
    void updateStatusShouldPersistNonDeletedStatus() {
        FileInfo file = new FileInfo();
        file.setId(1L);
        file.setStatus(FileStatusEnum.NORMAL.getValue());

        when(fileInfoService.getById(1L)).thenReturn(file);
        when(fileInfoService.updateById(file)).thenReturn(true);

        fileAdminService.updateStatus(1L, FileStatusEnum.BLOCKED.getValue());

        assertEquals(FileStatusEnum.BLOCKED.getValue(), file.getStatus());
        verify(fileInfoService).updateById(file);
    }
    @Test
    void updateStatusShouldRejectDeletedFileRecord() {
        FileInfo file = new FileInfo();
        file.setId(9L);
        file.setStatus(FileStatusEnum.DELETED.getValue());

        when(fileInfoService.getById(9L)).thenReturn(file);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileAdminService.updateStatus(9L, FileStatusEnum.NORMAL.getValue())
        );

        assertEquals(FileResultCode.FILE_STATUS_INVALID.getCode(), exception.getCode());
        verify(fileInfoService, never()).updateById(any());
    }

    @Test
    void pageFilesShouldRejectInvalidReferenceType() {
        FileAdminPageQuery query = new FileAdminPageQuery();
        query.setReferenceType("invalid");

        BusinessException exception = assertThrows(BusinessException.class, () -> fileAdminService.pageFiles(query));

        assertEquals(com.cybzacg.blogbackend.enums.error.ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(fileInfoService, never()).page(any(), any());
    }

    @Test
    void pageTasksShouldRejectInvalidTaskStatus() {
        FileTaskPageQuery query = new FileTaskPageQuery();
        query.setTaskStatus(99);

        BusinessException exception = assertThrows(BusinessException.class, () -> fileAdminService.pageTasks(query));

        assertEquals(FileResultCode.UPLOAD_TASK_STATUS_INVALID.getCode(), exception.getCode());
        verify(fileUploadTaskService, never()).page(any(), any());
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

        when(fileInfoService.getById(1L)).thenReturn(file);
        when(fileBusinessInfoService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(fileUploadTaskService.lambdaQuery()).thenReturn(taskQuery);
        when(taskQuery.eq(anySFunction(), any())).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task));
        when(fileChunkService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(fileUploadTaskService.removeByIds(List.of(11L))).thenReturn(true);
        when(fileInfoService.updateById(file)).thenReturn(true);

        fileAdminService.deleteFile(1L);

        verify(fileBusinessInfoService).remove(any(LambdaQueryWrapper.class));
        verify(fileChunkService).remove(any(LambdaQueryWrapper.class));
        verify(storageService).deleteTempFiles("upload-11");
        verify(fileUploadTaskService).removeByIds(List.of(11L));
        verify(storageService).delete("attachment/2026/03/demo.png");
        verify(fileInfoService).updateById(file);
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

        when(fileInfoService.getById(2L)).thenReturn(file);
        when(fileBusinessInfoService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(fileUploadTaskService.lambdaQuery()).thenReturn(taskQuery);
        when(taskQuery.eq(anySFunction(), any())).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task));
        when(fileChunkService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(storageManager.getStorageService("local-test")).thenReturn(storageService);
        when(fileUploadTaskService.removeByIds(List.of(22L))).thenReturn(true);
        when(fileInfoService.updateById(file)).thenReturn(true);
        doThrow(new RuntimeException("temp cleanup failed")).when(storageService).deleteTempFiles("upload-22");
        doThrow(new RuntimeException("physical delete failed")).when(storageService).delete("attachment/2026/03/demo-2.png");

        assertDoesNotThrow(() -> fileAdminService.deleteFile(2L));

        verify(fileBusinessInfoService).remove(any(LambdaQueryWrapper.class));
        verify(fileChunkService).remove(any(LambdaQueryWrapper.class));
        verify(fileUploadTaskService).removeByIds(List.of(22L));
        verify(fileInfoService).updateById(file);
        assertEquals(Integer.valueOf(0), file.getReferenceCount());
        assertEquals(FileStatusEnum.DELETED.getValue(), file.getStatus());
    }

    @SuppressWarnings("unchecked")
    private static <T> SFunction<T, ?> anySFunction() {
        return (SFunction<T, ?>) any(SFunction.class);
    }
}



