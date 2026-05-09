package com.cybzacg.blogbackend.module.auth.rbac;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysRole;
import com.cybzacg.blogbackend.domain.auth.SysRoleMenu;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRoleRepository;
import com.cybzacg.blogbackend.module.auth.rbac.convert.RbacAdminModelConvert;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysRoleSaveRequest;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysMenuRepository;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleMenuRepository;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.rbac.service.impl.RbacAssociationFactory;
import com.cybzacg.blogbackend.module.auth.rbac.service.impl.SysRoleAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysRoleAdminServiceImplTest {
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private SysRoleMenuRepository sysRoleMenuRepository;
    @Mock
    private SysUserRoleRepository sysUserRoleRepository;
    @Mock
    private SysMenuRepository sysMenuRepository;
    @Mock
    private RbacAdminModelConvert rbacAdminModelConvert;
    @Mock
    private RbacAssociationFactory rbacAssociationFactory;

    private SysRoleAdminServiceImpl sysRoleAdminService;

    @BeforeEach
    void setUp() {
        sysRoleAdminService = new SysRoleAdminServiceImpl(
                sysRoleRepository,
                sysRoleMenuRepository,
                sysUserRoleRepository,
                sysMenuRepository,
                rbacAdminModelConvert,
                rbacAssociationFactory
        );
    }

    @Test
    void pageRolesShouldReturnMappedPageResult() {
        SysRolePageQuery query = new SysRolePageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setName("admin");
        role.setIsDeleted(0);
        Page<SysRole> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(role));

        when(sysRoleRepository.pageByAdminConditions(query)).thenReturn(page);
        when(sysRoleMenuRepository.findMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));
        SysRoleAdminVO vo = new SysRoleAdminVO();
        vo.setId(1L);
        when(rbacAdminModelConvert.toRoleVO(role, List.of(10L, 20L))).thenReturn(vo);

        PageResult<SysRoleAdminVO> result = sysRoleAdminService.pageRoles(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getRecords().get(0).getId());
    }

    @Test
    void createRoleShouldSaveAndReturnVO() {
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("editor");
        request.setCode("EDITOR");

        when(sysRoleRepository.existsActiveByName("editor", null)).thenReturn(false);
        when(sysRoleRepository.existsActiveByCode("EDITOR", null)).thenReturn(false);

        SysRole mappedRole = new SysRole();
        when(rbacAdminModelConvert.toRole(request)).thenReturn(mappedRole);
        SysRoleAdminVO expectedVO = new SysRoleAdminVO();
        expectedVO.setName("editor");
        when(rbacAdminModelConvert.toRoleVO(mappedRole, List.of())).thenReturn(expectedVO);

        SysRoleAdminVO result = sysRoleAdminService.createRole(request);

        assertEquals("editor", result.getName());
        verify(sysRoleRepository).save(mappedRole);
    }

    @Test
    void updateRoleShouldUpdateAndReturnVO() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setCode("editor");
        role.setIsDeleted(0);

        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("updated");
        request.setCode("UPDATED");

        when(sysRoleRepository.getById(8L)).thenReturn(role);
        when(sysRoleRepository.existsActiveByName("updated", 8L)).thenReturn(false);
        when(sysRoleRepository.existsActiveByCode("UPDATED", 8L)).thenReturn(false);
        when(sysRoleMenuRepository.findMenuIdsByRoleId(8L)).thenReturn(List.of(10L));
        SysRoleAdminVO expectedVO = new SysRoleAdminVO();
        expectedVO.setId(8L);
        when(rbacAdminModelConvert.toRoleVO(role, List.of(10L))).thenReturn(expectedVO);

        SysRoleAdminVO result = sysRoleAdminService.updateRole(8L, request);

        assertEquals(8L, result.getId());
        verify(sysRoleRepository).updateById(role);
    }

    @Test
    void updateRoleShouldRejectSuperAdminRole() {
        SysRole role = buildSuperAdminRole();
        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("updated");
        request.setCode("UPDATED");

        when(sysRoleRepository.getById(1L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.updateRole(1L, request));

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("超级管理员角色不可修改", exception.getMessage());
        verify(sysRoleRepository, never()).updateById(any(SysRole.class));
    }

    @Test
    void updateStatusShouldRejectSuperAdminRole() {
        SysRole role = new SysRole();
        role.setId(99L);
        role.setCode("admin");
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(99L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.updateStatus(99L, 0));

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("超级管理员角色不可修改", exception.getMessage());
        verify(sysRoleRepository, never()).updateById(any(SysRole.class));
    }

    @Test
    void deleteRoleShouldRemoveRelationsAndDeleteRole() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setCode("editor");
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(8L)).thenReturn(role);

        sysRoleAdminService.deleteRole(8L);

        verify(sysRoleMenuRepository).deleteByRoleId(8L);
        verify(sysUserRoleRepository).deleteByRoleId(8L);
        verify(sysRoleRepository).removeById(8L);
    }

    @Test
    void deleteRoleShouldRejectSuperAdminRole() {
        SysRole role = buildSuperAdminRole();

        when(sysRoleRepository.getById(1L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.deleteRole(1L));

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("超级管理员角色不可修改", exception.getMessage());
        verify(sysRoleMenuRepository, never()).deleteByRoleId(anyLong());
        verify(sysUserRoleRepository, never()).deleteByRoleId(anyLong());
        verify(sysRoleRepository, never()).removeById(anyLong());
    }

    @Test
    void assignMenusShouldReplaceRoleMenusWhenAllMenusExist() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(8L)).thenReturn(role);
        when(sysMenuRepository.countByIds(anyList())).thenReturn(2L);
        when(rbacAssociationFactory.createRoleMenu(any(), any())).thenAnswer(inv -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(inv.getArgument(0));
            rm.setMenuId(inv.getArgument(1));
            return rm;
        });
        when(sysRoleMenuRepository.saveBatch(anyCollection())).thenReturn(true);

        sysRoleAdminService.assignMenus(8L, List.of(3L, 3L, 5L));

        verify(sysRoleMenuRepository).deleteByRoleId(8L);
        ArgumentCaptor<Collection<SysRoleMenu>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(sysRoleMenuRepository).saveBatch(captor.capture());
        assertEquals(List.of(3L, 5L), captor.getValue().stream().map(SysRoleMenu::getMenuId).toList());
    }

    @Test
    void assignMenusShouldRejectSuperAdminRole() {
        SysRole role = buildSuperAdminRole();

        when(sysRoleRepository.getById(1L)).thenReturn(role);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.assignMenus(1L, List.of(3L, 5L)));

        assertEquals(ResultErrorCode.FORBIDDEN.getCode(), exception.getCode());
        assertEquals("超级管理员角色不可修改", exception.getMessage());
        verify(sysRoleMenuRepository, never()).deleteByRoleId(anyLong());
        verify(sysRoleMenuRepository, never()).saveBatch(anyCollection());
    }

    @Test
    void assignMenusShouldRejectUnknownMenuId() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(8L)).thenReturn(role);
        when(sysMenuRepository.countByIds(List.of(3L, 5L))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysRoleAdminService.assignMenus(8L, List.of(3L, 5L)));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("存在无效菜单", exception.getMessage());
        verify(sysRoleMenuRepository, never()).saveBatch(anyCollection());
    }

    @Test
    void assignMenusShouldAllowEmptyMenuList() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setCode("editor");
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(8L)).thenReturn(role);

        sysRoleAdminService.assignMenus(8L, Collections.emptyList());

        verify(sysRoleMenuRepository).deleteByRoleId(8L);
        verify(sysRoleMenuRepository, never()).saveBatch(anyCollection());
    }

    private SysRole buildSuperAdminRole() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setCode("admin");
        role.setIsDeleted(0);
        return role;
    }
}
