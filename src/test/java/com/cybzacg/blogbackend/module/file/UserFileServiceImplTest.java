package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.module.file.model.admin.UserTaskVO;
import com.cybzacg.blogbackend.module.file.model.user.*;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.module.file.service.FileUploadService;
import com.cybzacg.blogbackend.module.file.service.UserFileQueryService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserFileServiceImplTest {
    @Mock
    private UserFileQueryService userFileQueryService;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private FileBusinessInfoRepository fileBusinessInfoRepository;
    @Mock
    private FileLifecycleService fileLifecycleService;

    private UserFileServiceImpl userFileService;

    @BeforeEach
    void setUp() {
        userFileService = new UserFileServiceImpl(
                userFileQueryService,
                fileUploadService,
                fileBusinessInfoRepository,
                fileLifecycleService
        );
    }

    @Test
    void initUploadTaskShouldDelegateToFileUploadService() {
        FileUploadInitRequest request = new FileUploadInitRequest();
        request.setOriginalName("avatar.png");
        request.setFileSize(1024L);
        request.setFileMd5("abc123");
        request.setCategory("avatar");
        request.setReferenceType("avatar");
        request.setReferenceId(7L);

        UserTaskVO taskVO = new UserTaskVO();
        taskVO.setTaskId(21L);
        taskVO.setUploadId("upload-123");
        taskVO.setChunkSize(1024L);
        taskVO.setTotalChunks(1);
        taskVO.setUploadMode(0);
        taskVO.setQuickUploadAvailable(true);
        taskVO.setCompleted(false);
        taskVO.setTaskStatus(0);

        when(fileUploadService.initUploadTask(any(), any())).thenReturn(taskVO);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            FileUploadInitVO result = userFileService.initUploadTask(request, "127.0.0.1");

            assertEquals(21L, result.getTaskId());
            assertEquals("upload-123", result.getUploadId());
            verify(fileUploadService).initUploadTask(eq(7L), any());
        }
    }

    @Test
    void quickCheckShouldDelegateToFileUploadService() {
        UserTaskVO taskVO = new UserTaskVO();
        taskVO.setTaskId(8L);
        taskVO.setUploadId("upload-quick");
        taskVO.setQuickUpload(true);
        taskVO.setTaskStatus(2);

        when(fileUploadService.quickCheck(any(), any())).thenReturn(taskVO);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            FileUploadResultVO result = userFileService.quickCheck("upload-quick", "127.0.0.1");

            assertEquals(8L, result.getTaskId());
            assertTrue(result.getQuickUpload());
            verify(fileUploadService).quickCheck(eq(7L), eq("upload-quick"));
        }
    }

    @Test
    void pageMyFilesShouldDelegateToUserFileQueryService() {
        UserFilePageQuery query = new UserFilePageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        PageResult<UserFileVO> expectedResult = PageResult.<UserFileVO>builder()
                .total(1L)
                .current(1L)
                .size(10L)
                .records(List.of())
                .build();

        when(userFileQueryService.pageMyFiles(any(), any())).thenReturn(expectedResult);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            PageResult<UserFileVO> result = userFileService.pageMyFiles(query);

            assertEquals(1L, result.getTotal());
            verify(userFileQueryService).pageMyFiles(eq(7L), eq(query));
        }
    }

    @Test
    void pageMyUploadTasksShouldDelegateToUserFileQueryService() {
        UserFileTaskPageQuery query = new UserFileTaskPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        PageResult<UserFileTaskVO> expectedResult = PageResult.<UserFileTaskVO>builder()
                .total(0L)
                .current(1L)
                .size(10L)
                .records(List.of())
                .build();

        when(userFileQueryService.pageMyUploadTasks(any(), any())).thenReturn(expectedResult);

        try (MockedStatic<SecurityUtils> ignored = mockUser()) {
            PageResult<UserFileTaskVO> result = userFileService.pageMyUploadTasks(query);

            assertEquals(0L, result.getTotal());
            verify(userFileQueryService).pageMyUploadTasks(eq(7L), eq(query));
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
        }
    }

    private MockedStatic<SecurityUtils> mockUser() {
        MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::requireUserId).thenReturn(7L);
        return securityUtils;
    }
}
