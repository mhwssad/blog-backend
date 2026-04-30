package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.SysUserRole;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.rbac.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserSaveRequest;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRoleRepository;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.auth.account.token.TokenManager;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.rbac.service.impl.RbacAssociationFactory;
import com.cybzacg.blogbackend.module.auth.service.impl.SysUserAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import com.cybzacg.blogbackend.module.auth.account.token.TokenManager;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.auth.account.service.TwoFactorService;
import com.cybzacg.blogbackend.module.auth.notice.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.auth.rbac.service.impl.RbacAssociationFactory;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysUserAdminServiceImplTest {
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private SysUserRoleRepository sysUserRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RbacAdminModelMapper rbacAdminModelMapper;
    @Mock
    private RbacAssociationFactory rbacAssociationFactory;
    @Mock
    private UserNotificationPreferenceService userNotificationPreferenceService;
    @Mock
    private SuperAdminVerifier superAdminVerifier;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private SysAuditLogService sysAuditLogService;
    @Mock
    private TokenManager tokenManager;

    private SysUserAdminServiceImpl sysUserAdminService;

    @BeforeEach
    void setUp() {
        sysUserAdminService = new SysUserAdminServiceImpl(
                sysUserRepository,
                sysRoleRepository,
                sysUserRoleRepository,
                passwordEncoder,
                rbacAdminModelMapper,
                rbacAssociationFactory,
                userNotificationPreferenceService,
                superAdminVerifier,
                twoFactorService,
                sysAuditLogService,
                tokenManager
        );
    }

    @Test
    void pageUsersShouldReturnPageResult() {
        SysUserPageQuery query = new SysUserPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setDeletedFlag(0);
        Page<SysUser> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(user));

        when(sysUserRepository.pageByAdminConditions(query)).thenReturn(page);
        when(sysUserRoleRepository.findRoleIdsByUserId(1L)).thenReturn(List.of(1L, 2L));

        SysUserAdminVO vo = new SysUserAdminVO();
        vo.setId(1L);
        when(rbacAdminModelMapper.toUserVO(user, List.of(1L, 2L))).thenReturn(vo);

        PageResult<SysUserAdminVO> result = sysUserAdminService.pageUsers(query);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getRecords().get(0).getId());
    }

    @Test
    void getUserShouldReturnUserVOWithRoleIds() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserRepository.getById(1L)).thenReturn(user);
        when(sysUserRoleRepository.findRoleIdsByUserId(1L)).thenReturn(List.of(1L, 2L));

        SysUserAdminVO expectedVO = new SysUserAdminVO();
        expectedVO.setId(1L);
        expectedVO.setRoleIds(List.of(1L, 2L));
        when(rbacAdminModelMapper.toUserVO(user, List.of(1L, 2L))).thenReturn(expectedVO);

        SysUserAdminVO result = sysUserAdminService.getUser(1L);

        assertEquals(1L, result.getId());
        assertEquals(List.of(1L, 2L), result.getRoleIds());
    }

    @Test
    void createUserShouldSaveAndReturnVO() {
        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("newuser");
        request.setPassword("123456");
        request.setEmail("test@example.com");
        request.setPhone("13800138000");

        SysUser mappedUser = new SysUser();
        when(rbacAdminModelMapper.toUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_pwd");
        when(sysUserRepository.existsActiveByUsername("newuser", null)).thenReturn(false);
        when(sysUserRepository.existsActiveByEmail("test@example.com", null)).thenReturn(false);
        when(sysUserRepository.existsActiveByPhone("13800138000", null)).thenReturn(false);

        SysUserAdminVO expectedVO = new SysUserAdminVO();
        expectedVO.setUsername("newuser");
        when(rbacAdminModelMapper.toUserVO(mappedUser, List.of())).thenReturn(expectedVO);

        SysUserAdminVO result = sysUserAdminService.createUser(request);

        assertEquals("newuser", result.getUsername());
        verify(sysUserRepository).save(mappedUser);
        assertEquals("encoded_pwd", mappedUser.getPassword());
    }

    @Test
    void createUserShouldThrowWhenUsernameAlreadyExists() {
        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("existinguser");
        request.setPassword("123456");

        when(sysUserRepository.existsActiveByUsername("existinguser", null)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.createUser(request));

        assertEquals("用户名已存在", exception.getMessage());
        verify(sysUserRepository, never()).save(any());
    }

    @Test
    void updateUserShouldUpdateAndReturnVO() {
        SysUser existingUser = new SysUser();
        existingUser.setId(1L);
        existingUser.setDeletedFlag(0);

        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("updateduser");
        request.setEmail("updated@example.com");

        when(sysUserRepository.getById(1L)).thenReturn(existingUser);
        when(sysUserRepository.existsActiveByUsername("updateduser", 1L)).thenReturn(false);
        when(sysUserRepository.existsActiveByEmail("updated@example.com", 1L)).thenReturn(false);
        when(sysUserRoleRepository.findRoleIdsByUserId(1L)).thenReturn(List.of(2L));

        SysUserAdminVO expectedVO = new SysUserAdminVO();
        expectedVO.setId(1L);
        when(rbacAdminModelMapper.toUserVO(existingUser, List.of(2L))).thenReturn(expectedVO);

        SysUserAdminVO result = sysUserAdminService.updateUser(1L, request);

        assertEquals(1L, result.getId());
        verify(sysUserRepository).updateById(existingUser);
    }

    @Test
    void deleteUserShouldSoftDeleteAndRemoveRoles() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserRepository.getById(1L)).thenReturn(user);

        sysUserAdminService.deleteUser(1L);

        assertEquals(1, user.getDeletedFlag());
        verify(sysUserRepository).updateById(user);
        verify(sysUserRoleRepository).deleteByUserId(1L);
    }

    @Test
    void listRoleIdsShouldReturnRoleIdsForAvailableUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserRepository.getById(1L)).thenReturn(user);
        when(sysUserRoleRepository.findRoleIdsByUserId(1L)).thenReturn(List.of(1L, 2L, 3L));

        List<Long> result = sysUserAdminService.listRoleIds(1L);

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void assignRolesShouldReplaceUserRolesWhenAllRolesExist() {
        SysUser user = new SysUser();
        user.setId(6L);
        user.setDeletedFlag(0);

        when(sysUserRepository.getById(6L)).thenReturn(user);
        when(sysRoleRepository.countActiveByIds(anyList())).thenReturn(2L);
        when(rbacAssociationFactory.createUserRole(any(), any())).thenAnswer(inv -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(inv.getArgument(0));
            ur.setRoleId(inv.getArgument(1));
            return ur;
        });
        when(sysUserRoleRepository.saveBatch(anyCollection())).thenReturn(true);

        sysUserAdminService.assignRoles(6L, List.of(2L, 2L, 4L));

        verify(sysUserRoleRepository).deleteByUserId(6L);
        ArgumentCaptor<Collection<SysUserRole>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(sysUserRoleRepository).saveBatch(captor.capture());
        assertEquals(List.of(2L, 4L), captor.getValue().stream().map(SysUserRole::getRoleId).toList());
    }

    @Test
    void assignRolesShouldRejectUnknownRoleId() {
        SysUser user = new SysUser();
        user.setId(6L);
        user.setDeletedFlag(0);

        when(sysUserRepository.getById(6L)).thenReturn(user);
        when(sysRoleRepository.countActiveByIds(List.of(2L, 4L))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.assignRoles(6L, List.of(2L, 4L)));

        assertEquals("存在无效角色", exception.getMessage());
        verify(sysUserRoleRepository, never()).saveBatch(anyCollection());
    }

    @Test
    void assignRolesShouldAllowEmptyMenuList() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserRepository.getById(1L)).thenReturn(user);

        sysUserAdminService.assignRoles(1L, Collections.emptyList());

        verify(sysUserRoleRepository).deleteByUserId(1L);
        verify(sysUserRoleRepository, never()).saveBatch(anyCollection());
    }

    @Test
    void banUserByReportShouldBanTargetAndRecordAuditWithoutMfa() {
        SysUser target = new SysUser();
        target.setId(5L);
        target.setUsername("baduser");
        target.setStatus(1);
        target.setDeletedFlag(0);

        when(sysUserRepository.getById(5L)).thenReturn(target);

        sysUserAdminService.banUserByReport(1L, 5L, "举报封禁：违规内容", "127.0.0.1", "TestAgent");

        assertEquals(0, target.getStatus());
        verify(sysUserRepository).updateById(target);
        verify(tokenManager).invalidateUserSessions(5L);

        ArgumentCaptor<com.cybzacg.blogbackend.module.auth.model.common.SysAuditLogCreateRequest> captor =
                ArgumentCaptor.forClass(com.cybzacg.blogbackend.module.auth.model.common.SysAuditLogCreateRequest.class);
        verify(sysAuditLogService).record(captor.capture());
        var audit = captor.getValue();
        assertEquals(1L, audit.getOperatorUserId());
        assertEquals(5L, audit.getTargetUserId());
        assertEquals(com.cybzacg.blogbackend.enums.SysAuditOperationType.BAN_USER.getCode(), audit.getOperationType());
        assertEquals("举报封禁：违规内容", audit.getRemark());
        assertEquals(0, audit.getMfaPassed());
    }

    @Test
    void banUserByReportShouldRejectIfOperatorIsNotSuperAdmin() {
        doThrow(new com.cybzacg.blogbackend.exception.BusinessException(
                com.cybzacg.blogbackend.enums.error.ResultErrorCode.NOT_SUPER_ADMIN))
                .when(superAdminVerifier).requireSuperAdmin(1L);

        assertThrows(com.cybzacg.blogbackend.exception.BusinessException.class,
                () -> sysUserAdminService.banUserByReport(1L, 5L, "reason", "127.0.0.1", "UA"));

        verify(sysUserRepository, never()).updateById(any());
        verify(tokenManager, never()).invalidateUserSessions(anyLong());
        verify(sysAuditLogService, never()).record(any());
    }

    @Test
    void banUserByReportShouldRejectSelfBan() {
        doNothing().when(superAdminVerifier).requireSuperAdmin(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.banUserByReport(1L, 1L, "reason", "127.0.0.1", "UA"));

        assertEquals(com.cybzacg.blogbackend.enums.error.ResultErrorCode.CANNOT_MODIFY_SELF.getCode(), exception.getCode());
        verify(sysUserRepository, never()).updateById(any());
    }
}
