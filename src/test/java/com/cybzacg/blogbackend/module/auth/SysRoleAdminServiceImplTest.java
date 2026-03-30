package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleSaveRequest;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
    private LambdaQueryChainWrapper<SysMenu> menuQuery;
    @Mock
    private LambdaQueryChainWrapper<SysRole> roleQuery;

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

    // ==================== pageRoles ====================

    @Test
    void pageRolesShouldReturnMappedPageResult() {
        SysRolePageQuery query = new SysRolePageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setName("admin");
        role.setIsDeleted(0);
        Page<SysRole> page = new Page<>(1, 10);
        page.setRecords(List.of(role));
        page.setTotal(1);

        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<SysRole> pageQuery = org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        when(sysRoleService.lambdaQuery()).thenReturn(pageQuery);
        when(pageQuery.like(anyBoolean(), any(), any())).thenReturn(pageQuery);
        when(pageQuery.eq(any(), any())).thenReturn(pageQuery);
        when(pageQuery.eq(anyBoolean(), any(), any())).thenReturn(pageQuery);
        when(pageQuery.orderByAsc(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class))).thenReturn(pageQuery);
        when(pageQuery.page(any())).thenReturn(page);

        SysRoleAdminVO vo = new SysRoleAdminVO();
        vo.setId(1L);
        when(rbacAdminModelMapper.toRoleVO(eq(role), any())).thenReturn(vo);
        when(sysRoleMenuService.listMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));

        PageResult<SysRoleAdminVO> result = sysRoleAdminService.pageRoles(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getRecords().get(0).getId());
    }

    // ==================== getRole ====================

    @Test
    void getRoleShouldReturnVOWithMenuIds() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setName("admin");
        role.setIsDeleted(0);

        when(sysRoleService.getById(1L)).thenReturn(role);
        when(sysRoleMenuService.listMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));

        SysRoleAdminVO expectedVO = new SysRoleAdminVO();
        expectedVO.setId(1L);
        expectedVO.setMenuIds(List.of(10L, 20L));
        when(rbacAdminModelMapper.toRoleVO(role, List.of(10L, 20L))).thenReturn(expectedVO);

        SysRoleAdminVO result = sysRoleAdminService.getRole(1L);

        assertEquals(1L, result.getId());
        assertEquals(List.of(10L, 20L), result.getMenuIds());
    }

    @Test
    void getRoleShouldThrowWhenRoleNotFound() {
        when(sysRoleService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.getRole(999L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
    }

    @Test
    void getRoleShouldThrowWhenRoleIsDeleted() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(1);

        when(sysRoleService.getById(1L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.getRole(1L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
    }

    // ==================== createRole ====================

    @Test
    void createRoleShouldSaveAndReturnVO() {
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("editor");
        request.setCode("EDITOR");

        when(sysRoleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.ne(anyBoolean(), any(), any())).thenReturn(roleQuery);
        when(roleQuery.exists()).thenReturn(false);

        SysRole mappedRole = new SysRole();
        when(rbacAdminModelMapper.toRole(request)).thenReturn(mappedRole);

        SysRoleAdminVO expectedVO = new SysRoleAdminVO();
        expectedVO.setName("editor");
        when(rbacAdminModelMapper.toRoleVO(mappedRole, List.of())).thenReturn(expectedVO);

        SysRoleAdminVO result = sysRoleAdminService.createRole(request);

        assertNotNull(result);
        assertEquals("editor", result.getName());
        verify(sysRoleService).save(mappedRole);
        assertEquals(0, mappedRole.getIsDeleted());
        verify(rbacAdminModelMapper).updateRole(request, mappedRole);
    }

    @Test
    void createRoleShouldThrowWhenNameExists() {
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("admin");
        request.setCode("NEW_CODE");

        when(sysRoleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.ne(anyBoolean(), any(), any())).thenReturn(roleQuery);
        // First call (name check) returns true -> name already exists
        // Second call (code check) returns false
        when(roleQuery.exists()).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.createRole(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色名称已存在", exception.getMessage());
        verify(sysRoleService, never()).save(any());
    }

    @Test
    void createRoleShouldThrowWhenCodeExists() {
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("new_role");
        request.setCode("ADMIN");

        when(sysRoleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.ne(anyBoolean(), any(), any())).thenReturn(roleQuery);
        // First call (name check) passes, second call (code check) fails
        when(roleQuery.exists()).thenReturn(false, true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.createRole(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色编码已存在", exception.getMessage());
        verify(sysRoleService, never()).save(any());
    }

    // ==================== updateRole ====================

    @Test
    void updateRoleShouldUpdateAndReturnVO() {
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("updated");
        request.setCode("UPDATED");

        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(1L)).thenReturn(role);
        when(sysRoleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.ne(anyBoolean(), any(), any())).thenReturn(roleQuery);
        when(roleQuery.exists()).thenReturn(false, false);

        when(sysRoleMenuService.listMenuIdsByRoleId(1L)).thenReturn(List.of(10L));
        SysRoleAdminVO expectedVO = new SysRoleAdminVO();
        expectedVO.setId(1L);
        when(rbacAdminModelMapper.toRoleVO(role, List.of(10L))).thenReturn(expectedVO);

        SysRoleAdminVO result = sysRoleAdminService.updateRole(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(sysRoleService).updateById(role);
        verify(rbacAdminModelMapper).updateRole(request, role);
    }

    @Test
    void updateRoleShouldThrowWhenRoleNotFound() {
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("updated");
        request.setCode("UPDATED");

        when(sysRoleService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.updateRole(999L, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
        verify(sysRoleService, never()).updateById(any());
    }

    @Test
    void updateRoleShouldThrowWhenNameDuplicated() {
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("duplicate_name");
        request.setCode("CODE");

        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(1L)).thenReturn(role);
        when(sysRoleService.lambdaQuery()).thenReturn(roleQuery);
        when(roleQuery.eq(any(), any())).thenReturn(roleQuery);
        when(roleQuery.ne(anyBoolean(), any(), any())).thenReturn(roleQuery);
        when(roleQuery.exists()).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.updateRole(1L, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色名称已存在", exception.getMessage());
        verify(sysRoleService, never()).updateById(any());
    }

    // ==================== updateStatus ====================

    @Test
    void updateStatusShouldSetStatusAndUpdate() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);
        role.setStatus(1);

        when(sysRoleService.getById(1L)).thenReturn(role);

        sysRoleAdminService.updateStatus(1L, 0);

        assertEquals(0, role.getStatus());
        verify(sysRoleService).updateById(role);
    }

    @Test
    void updateStatusShouldThrowWhenRoleNotFound() {
        when(sysRoleService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.updateStatus(999L, 0));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
        verify(sysRoleService, never()).updateById(any());
    }

    // ==================== deleteRole ====================

    @Test
    void deleteRoleShouldRemoveRoleMenuUserRoleAndHardDeleteRole() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(1L)).thenReturn(role);

        sysRoleAdminService.deleteRole(1L);

        verify(sysRoleMenuService).removeByRoleId(1L);
        verify(sysUserRoleService).removeByRoleId(1L);
        verify(sysRoleService).removeById(1L);
    }

    @Test
    void deleteRoleShouldThrowWhenRoleNotFound() {
        when(sysRoleService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.deleteRole(999L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
        verify(sysRoleMenuService, never()).removeByRoleId(any());
        verify(sysUserRoleService, never()).removeByRoleId(any());
        verify(sysRoleService, never()).removeById(any(java.io.Serializable.class));
    }

    @Test
    void deleteRoleShouldThrowWhenRoleIsDeleted() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(1);

        when(sysRoleService.getById(1L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.deleteRole(1L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
        verify(sysRoleMenuService, never()).removeByRoleId(any());
    }

    // ==================== listMenuIds ====================

    @Test
    void listMenuIdsShouldReturnMenuIdsForAvailableRole() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(1L)).thenReturn(role);
        when(sysRoleMenuService.listMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L, 30L));

        List<Long> result = sysRoleAdminService.listMenuIds(1L);

        assertEquals(3, result.size());
        assertEquals(List.of(10L, 20L, 30L), result);
    }

    @Test
    void listMenuIdsShouldThrowWhenRoleNotFound() {
        when(sysRoleService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.listMenuIds(999L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
        verify(sysRoleMenuService, never()).listMenuIdsByRoleId(any());
    }

    // ==================== assignMenus (existing tests) ====================

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

    @Test
    void assignMenusShouldThrowWhenRoleNotFound() {
        when(sysRoleService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.assignMenus(999L, List.of(1L, 2L)));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("角色不存在", exception.getMessage());
        verify(sysRoleMenuService, never()).replaceRoleMenus(any(), any());
    }

    @Test
    void assignMenusShouldAllowEmptyMenuList() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);

        when(sysRoleService.getById(1L)).thenReturn(role);

        sysRoleAdminService.assignMenus(1L, Collections.emptyList());

        verify(sysMenuService, never()).lambdaQuery();
        verify(sysRoleMenuService).replaceRoleMenus(1L, Collections.emptyList());
    }
}




