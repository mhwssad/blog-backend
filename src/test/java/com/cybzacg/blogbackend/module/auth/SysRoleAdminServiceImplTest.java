package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserRoleService;
import com.cybzacg.blogbackend.module.auth.service.impl.SysRoleAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SysRoleAdminServiceImplTest {
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysRoleMenuService sysRoleMenuService;
    @Mock
    private SysUserRoleService sysUserRoleService;
    @Mock
    private SysMenuService sysMenuService;
    @Mock
    private RbacAdminModelMapper rbacAdminModelMapper;
    @Mock
    private LambdaQueryChainWrapper<com.cybzacg.blogbackend.domain.SysMenu> menuQuery;

    private SysRoleAdminServiceImpl sysRoleAdminService;

    @BeforeEach
    void setUp() {
        sysRoleAdminService = new SysRoleAdminServiceImpl(
                sysRoleService,
                sysRoleMenuService,
                sysUserRoleService,
                sysMenuService,
                rbacAdminModelMapper
        );
    }

    @Test
    void assignMenusShouldReplaceRoleMenusWhenAllMenusExist() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(8L)).thenReturn(role);
        when(sysMenuService.lambdaQuery()).thenReturn(menuQuery);
        when(menuQuery.in(any(), anyCollection())).thenReturn(menuQuery);
        when(menuQuery.count()).thenReturn(2L);

        sysRoleAdminService.assignMenus(8L, List.of(3L, 3L, 5L));

        verify(sysRoleMenuService).replaceRoleMenus(8L, List.of(3L, 3L, 5L));
    }

    @Test
    void assignMenusShouldRejectNullMenuId() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(8L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.assignMenus(8L, Arrays.asList(3L, null, 5L)));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("菜单ID不能为空", exception.getMessage());
        verify(sysMenuService, never()).lambdaQuery();
        verify(sysRoleMenuService, never()).replaceRoleMenus(any(), any());
    }

    @Test
    void assignMenusShouldRejectUnknownMenuId() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(8L)).thenReturn(role);
        when(sysMenuService.lambdaQuery()).thenReturn(menuQuery);
        when(menuQuery.in(any(), anyCollection())).thenReturn(menuQuery);
        when(menuQuery.count()).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.assignMenus(8L, List.of(3L, 5L)));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("存在无效菜单", exception.getMessage());
        verify(sysRoleMenuService, never()).replaceRoleMenus(any(), any());
    }
}
