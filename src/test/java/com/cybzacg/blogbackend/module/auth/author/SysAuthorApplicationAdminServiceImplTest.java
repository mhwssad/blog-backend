package com.cybzacg.blogbackend.module.auth.author;

import com.cybzacg.blogbackend.domain.SysAuthorApplication;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.auth.AuthorApplicationStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.author.convert.AuthorApplicationModelMapper;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminReviewRequest;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationRepairRequest;
import com.cybzacg.blogbackend.module.auth.author.repository.SysAuthorApplicationRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.auth.author.service.impl.SysAuthorApplicationAdminServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysAuthorApplicationAdminServiceImplTest {
    @Mock
    private SysAuthorApplicationRepository sysAuthorApplicationRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private AuthorPermissionService authorPermissionService;
    @Mock
    private AuthorApplicationModelMapper authorApplicationModelMapper;

    private SysAuthorApplicationAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysAuthorApplicationAdminServiceImpl(
                sysAuthorApplicationRepository,
                sysUserRepository,
                authorPermissionService,
                authorApplicationModelMapper
        );
    }

    @Test
    void reviewApplicationShouldApproveAndGrantAuthorRole() {
        SysAuthorApplication application = buildApplication(1L, 10L, AuthorApplicationStatusEnum.PENDING.getValue());
        SysAuthorApplicationAdminReviewRequest request = new SysAuthorApplicationAdminReviewRequest();
        request.setReviewStatus(AuthorApplicationStatusEnum.APPROVED.getValue());
        request.setReviewComment("符合条件，通过");

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(99L)) {
            when(sysAuthorApplicationRepository.getById(1L)).thenReturn(application);

            service.reviewApplication(1L, request);

            assertEquals(AuthorApplicationStatusEnum.APPROVED.getValue(), application.getApplyStatus());
            assertEquals("符合条件，通过", application.getReviewComment());
            assertEquals(99L, application.getReviewerId());
            assertNotNull(application.getReviewedAt());
            verify(sysAuthorApplicationRepository).updateById(application);
            verify(authorPermissionService).grantAuthorRole(10L);
        }
    }

    @Test
    void reviewApplicationShouldRejectAndKeepUserRole() {
        SysAuthorApplication application = buildApplication(1L, 10L, AuthorApplicationStatusEnum.PENDING.getValue());
        SysAuthorApplicationAdminReviewRequest request = new SysAuthorApplicationAdminReviewRequest();
        request.setReviewStatus(AuthorApplicationStatusEnum.REJECTED.getValue());
        request.setReviewComment("条件不符");

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(99L)) {
            when(sysAuthorApplicationRepository.getById(1L)).thenReturn(application);

            service.reviewApplication(1L, request);

            assertEquals(AuthorApplicationStatusEnum.REJECTED.getValue(), application.getApplyStatus());
            assertEquals("条件不符", application.getReviewComment());
            assertEquals(99L, application.getReviewerId());
            verify(sysAuthorApplicationRepository).updateById(application);
            verify(authorPermissionService, never()).grantAuthorRole(any());
        }
    }

    @Test
    void reviewApplicationShouldRejectNonPendingStatus() {
        SysAuthorApplication application = buildApplication(1L, 10L, AuthorApplicationStatusEnum.APPROVED.getValue());
        SysAuthorApplicationAdminReviewRequest request = new SysAuthorApplicationAdminReviewRequest();
        request.setReviewStatus(AuthorApplicationStatusEnum.APPROVED.getValue());

        when(sysAuthorApplicationRepository.getById(1L)).thenReturn(application);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.reviewApplication(1L, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("当前申请状态不可审核", exception.getMessage());
        verify(sysAuthorApplicationRepository, never()).updateById(any());
    }

    @Test
    void repairApplicationShouldSyncAuthorRole() {
        SysAuthorApplication application = buildApplication(1L, 10L, AuthorApplicationStatusEnum.REJECTED.getValue());
        SysAuthorApplicationRepairRequest request = new SysAuthorApplicationRepairRequest();
        request.setTargetStatus(AuthorApplicationStatusEnum.APPROVED.getValue());
        request.setReviewComment("管理员修正：实际已通过");

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(99L)) {
            when(sysAuthorApplicationRepository.getById(1L)).thenReturn(application);

            service.repairApplication(1L, request);

            ArgumentCaptor<SysAuthorApplication> captor = ArgumentCaptor.forClass(SysAuthorApplication.class);
            verify(sysAuthorApplicationRepository).updateById(captor.capture());
            SysAuthorApplication updated = captor.getValue();
            assertEquals(AuthorApplicationStatusEnum.APPROVED.getValue(), updated.getApplyStatus());
            assertEquals("管理员修正：实际已通过", updated.getReviewComment());
            assertEquals(99L, updated.getReviewerId());
            assertNotNull(updated.getReviewedAt());

            verify(authorPermissionService).grantAuthorRole(10L);
            verify(authorPermissionService, never()).revokeAuthorRole(any());
        }
    }

    // ---- helper fixtures ----

    private SysAuthorApplication buildApplication(Long id, Long userId, Integer applyStatus) {
        SysAuthorApplication application = new SysAuthorApplication();
        application.setId(id);
        application.setUserId(userId);
        application.setApplyStatus(applyStatus);
        return application;
    }
}
