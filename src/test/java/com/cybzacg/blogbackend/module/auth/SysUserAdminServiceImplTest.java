package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import com.cybzacg.blogbackend.module.auth.service.impl.SysUserAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserAdminServiceImplTest {
    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysUserRoleService sysUserRoleService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RbacAdminModelMapper rbacAdminModelMapper;
    @Mock
    private LambdaQueryChainWrapper<com.cybzacg.blogbackend.domain.SysRole> roleQuery;

    private SysUserAdminServiceImpl sysUserAdminService;

    @BeforeEach
    void setUp() {
        sysUserAdminService = new SysUserAdminServiceImpl(
                sysUserService,
                sysRoleService,
                sysUserRoleService,
                passwordEncoder,
                rbacAdminModelMapper
        );
    }

    @Test
    void assignRolesShouldReplaceUserRolesWhenAllRolesExist() {
        SysUser user = new SysUser();
        user.setId(6L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(6L)).thenReturn(user);
        when(sysRoleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.in(any(), anyCollection())).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.count()).thenReturn(2L);

        sysUserAdminService.assignRoles(6L, List.of(2L, 2L, 4L));

        verify(sysUserRoleService).replaceUserRoles(6L, List.of(2L, 2L, 4L));
    }

    @Test
    void assignRolesShouldRejectNullRoleId() {
        SysUser user = new SysUser();
        user.setId(6L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(6L)).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.assignRoles(6L, Arrays.asList(2L, null, 4L)));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色ID不能为空", exception.getMessage());
        verify(sysRoleService, never()).lambdaQuery();
        verify(sysUserRoleService, never()).replaceUserRoles(any(), any());
    }

    @Test
    void assignRolesShouldRejectUnknownRoleId() {
        SysUser user = new SysUser();
        user.setId(6L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(6L)).thenReturn(user);
        when(sysRoleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.in(any(), anyCollection())).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.count()).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.assignRoles(6L, List.of(2L, 4L)));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("存在无效角色", exception.getMessage());
        verify(sysUserRoleService, never()).replaceUserRoles(any(), any());
    }
}
