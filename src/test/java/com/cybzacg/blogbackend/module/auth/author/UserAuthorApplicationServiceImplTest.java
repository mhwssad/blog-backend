package com.cybzacg.blogbackend.module.auth.author;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysAuthorApplication;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.auth.AuthorApplicationStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.author.convert.AuthorApplicationModelMapper;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationPageQuery;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationVO;
import com.cybzacg.blogbackend.module.auth.author.repository.SysAuthorApplicationRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.auth.author.service.impl.UserAuthorApplicationServiceImpl;
import com.cybzacg.blogbackend.support.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthorApplicationServiceImplTest {
    @Mock
    private SysAuthorApplicationRepository sysAuthorApplicationRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private AuthorPermissionService authorPermissionService;
    @Mock
    private AuthorApplicationModelMapper authorApplicationModelMapper;

    private UserAuthorApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserAuthorApplicationServiceImpl(
                sysAuthorApplicationRepository,
                sysUserRepository,
                authorPermissionService,
                authorApplicationModelMapper
        );
    }

    @Test
    void submitApplicationShouldCreateRecordSuccessfully() {
        UserAuthorApplicationSubmitRequest request = buildSubmitRequest();
        SysUser user = buildActiveUser(1L);
        SysAuthorApplication mapped = new SysAuthorApplication();
        UserAuthorApplicationVO expectedVO = buildVO(1L, AuthorApplicationStatusEnum.PENDING.getValue());

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(sysUserRepository.getById(1L)).thenReturn(user);
            when(authorPermissionService.hasAuthorRole(1L)).thenReturn(false);
            when(sysAuthorApplicationRepository.findLatestByUserId(1L)).thenReturn(null);
            when(authorApplicationModelMapper.toApplication(request)).thenReturn(mapped);
            when(authorApplicationModelMapper.toUserVO(mapped)).thenReturn(expectedVO);

            UserAuthorApplicationVO result = service.submitApplication(request);

            assertSame(expectedVO, result);
            verify(sysAuthorApplicationRepository).save(mapped);
            assertEquals(AuthorApplicationStatusEnum.PENDING.getValue(), mapped.getApplyStatus());
            assertEquals(1L, mapped.getUserId());
            assertNotNull(mapped.getSubmittedAt());
        }
    }

    @Test
    void submitApplicationShouldRejectDuplicateWhenPendingExists() {
        UserAuthorApplicationSubmitRequest request = buildSubmitRequest();
        SysUser user = buildActiveUser(1L);
        SysAuthorApplication pending = new SysAuthorApplication();
        pending.setApplyStatus(AuthorApplicationStatusEnum.PENDING.getValue());

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(sysUserRepository.getById(1L)).thenReturn(user);
            when(authorPermissionService.hasAuthorRole(1L)).thenReturn(false);
            when(sysAuthorApplicationRepository.findLatestByUserId(1L)).thenReturn(pending);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.submitApplication(request));

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("当前已有待审核申请，请勿重复提交", exception.getMessage());
            verify(sysAuthorApplicationRepository, never()).save(any());
        }
    }

    @Test
    void submitApplicationShouldRejectWhenAlreadyAuthor() {
        UserAuthorApplicationSubmitRequest request = buildSubmitRequest();
        SysUser user = buildActiveUser(1L);

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(sysUserRepository.getById(1L)).thenReturn(user);
            when(authorPermissionService.hasAuthorRole(1L)).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.submitApplication(request));

            assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
            assertEquals("当前用户已具备作者权限，无需重复申请", exception.getMessage());
            verify(sysAuthorApplicationRepository, never()).findLatestByUserId(any());
        }
    }

    @Test
    void submitApplicationShouldResubmitWhenNeedMoreInfo() {
        UserAuthorApplicationSubmitRequest request = buildSubmitRequest();
        SysUser user = buildActiveUser(1L);
        SysAuthorApplication needMoreInfo = new SysAuthorApplication();
        needMoreInfo.setId(10L);
        needMoreInfo.setApplyStatus(AuthorApplicationStatusEnum.NEED_MORE_INFO.getValue());
        needMoreInfo.setReviewerId(99L);
        needMoreInfo.setReviewComment("请补充简介");
        UserAuthorApplicationVO expectedVO = buildVO(10L, AuthorApplicationStatusEnum.PENDING.getValue());

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(sysUserRepository.getById(1L)).thenReturn(user);
            when(authorPermissionService.hasAuthorRole(1L)).thenReturn(false);
            when(sysAuthorApplicationRepository.findLatestByUserId(1L)).thenReturn(needMoreInfo);
            when(authorApplicationModelMapper.toUserVO(needMoreInfo)).thenReturn(expectedVO);

            UserAuthorApplicationVO result = service.submitApplication(request);

            assertSame(expectedVO, result);
            verify(authorApplicationModelMapper).updateApplication(request, needMoreInfo);
            assertEquals(AuthorApplicationStatusEnum.PENDING.getValue(), needMoreInfo.getApplyStatus());
            assertNull(needMoreInfo.getReviewerId());
            assertNull(needMoreInfo.getReviewComment());
            assertNull(needMoreInfo.getReviewedAt());
            assertNotNull(needMoreInfo.getSubmittedAt());
            verify(sysAuthorApplicationRepository).updateById(needMoreInfo);
            verify(sysAuthorApplicationRepository, never()).save(any());
        }
    }

    @Test
    void getLatestApplicationShouldReturnNullWhenNoneExist() {
        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(sysAuthorApplicationRepository.findLatestByUserId(1L)).thenReturn(null);

            UserAuthorApplicationVO result = service.getLatestApplication();

            assertNull(result);
        }
    }

    @Test
    void pageMyApplicationsShouldReturnPagedResults() {
        UserAuthorApplicationPageQuery query = new UserAuthorApplicationPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        SysAuthorApplication app1 = new SysAuthorApplication();
        app1.setId(1L);
        SysAuthorApplication app2 = new SysAuthorApplication();
        app2.setId(2L);

        Page<SysAuthorApplication> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(app1, app2));

        UserAuthorApplicationVO vo1 = buildVO(1L, AuthorApplicationStatusEnum.PENDING.getValue());
        UserAuthorApplicationVO vo2 = buildVO(2L, AuthorApplicationStatusEnum.APPROVED.getValue());

        try (MockedStatic<?> ignored = SecurityTestUtils.mockUserId(1L)) {
            when(sysAuthorApplicationRepository.pageByUserId(eq(1L), eq(1L), eq(10L))).thenReturn(page);
            when(authorApplicationModelMapper.toUserVO(app1)).thenReturn(vo1);
            when(authorApplicationModelMapper.toUserVO(app2)).thenReturn(vo2);

            PageResult<UserAuthorApplicationVO> result = service.pageMyApplications(query);

            assertEquals(2, result.getTotal());
            assertEquals(2, result.getRecords().size());
            assertSame(vo1, result.getRecords().get(0));
            assertSame(vo2, result.getRecords().get(1));
        }
    }

    // ---- helper fixtures ----

    private SysUser buildActiveUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(1);
        user.setDeletedFlag(0);
        return user;
    }

    private UserAuthorApplicationSubmitRequest buildSubmitRequest() {
        UserAuthorApplicationSubmitRequest request = new UserAuthorApplicationSubmitRequest();
        request.setApplyReason("我想成为作者");
        request.setContentDirection("Java后端");
        request.setIntroduction("5年开发经验");
        return request;
    }

    private UserAuthorApplicationVO buildVO(Long id, Integer applyStatus) {
        UserAuthorApplicationVO vo = new UserAuthorApplicationVO();
        vo.setId(id);
        vo.setApplyStatus(applyStatus);
        return vo;
    }
}
