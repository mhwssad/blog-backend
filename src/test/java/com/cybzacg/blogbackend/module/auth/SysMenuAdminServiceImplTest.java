package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleMenuService;
import com.cybzacg.blogbackend.module.auth.service.impl.SysMenuAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysMenuAdminServiceImplTest {
    @Mock
    private SysMenuService sysMenuService;
    @Mock
    private SysRoleMenuService sysRoleMenuService;
    @Mock
    private RbacAdminModelMapper rbacAdminModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysMenu> menuQuery;
    @Mock
    private LambdaQueryChainWrapper<SysMenu> childQuery;
    @Mock
    private LambdaQueryChainWrapper<SysMenu> grandChildQuery;
    @Mock
    private LambdaQueryChainWrapper<SysMenu> existsQuery;

    private SysMenuAdminServiceImpl sysMenuAdminService;

    @BeforeEach
    void setUp() {
        sysMenuAdminService = new SysMenuAdminServiceImpl(sysMenuService, sysRoleMenuService, rbacAdminModelMapper);
        mockMenuMappings();
    }

    @Test
    void listMenuTreeShouldBuildHierarchicalTree() {
        SysMenu root = menu(1L, 0L, "0", "系统管理", MenuConstants.TYPE_CATALOG);
        SysMenu child = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        SysMenu button = menu(3L, 2L, "0,1,2", "新增", MenuConstants.TYPE_BUTTON);

        when(sysMenuService.lambdaQuery()).thenReturn(menuQuery);
        when(menuQuery.orderByAsc(any(SFunction.class))).thenReturn(menuQuery);
        when(menuQuery.list()).thenReturn(List.of(root, child, button));

        List<SysMenuAdminVO> result = sysMenuAdminService.listMenuTree();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(2L, result.get(0).getChildren().get(0).getId());
        assertEquals(1, result.get(0).getChildren().get(0).getChildren().size());
        assertEquals(3L, result.get(0).getChildren().get(0).getChildren().get(0).getId());
    }

    @Test
    void getMenuShouldThrowWhenMenuNotFound() {
        when(sysMenuService.getById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysMenuAdminService.getMenu(99L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("菜单不存在", exception.getMessage());
    }

    @Test
    void createMenuShouldSaveRootMenuWithRootTreePath() {
        SysMenuSaveRequest request = menuSaveRequest(0L, "系统管理", MenuConstants.TYPE_CATALOG);
        SysMenu mapped = new SysMenu();
        stubSaveMenu();
        when(rbacAdminModelMapper.toMenu(request)).thenReturn(mapped);
        when(rbacAdminModelMapper.toMenuVO(mapped)).thenAnswer(invocation -> toMenuVO(invocation.getArgument(0)));

        SysMenuAdminVO result = sysMenuAdminService.createMenu(request);

        assertEquals(100L, result.getId());
        assertEquals("0", mapped.getTreePath());
        verify(sysMenuService).save(mapped);
        verify(rbacAdminModelMapper).updateMenu(request, mapped);
    }

    @Test
    void createMenuShouldSaveChildMenuWithParentTreePath() {
        SysMenuSaveRequest request = menuSaveRequest(10L, "用户管理", MenuConstants.TYPE_MENU);
        SysMenu parent = menu(10L, 0L, "0", "系统管理", MenuConstants.TYPE_CATALOG);
        SysMenu mapped = new SysMenu();

        stubSaveMenu();
        when(sysMenuService.getById(10L)).thenReturn(parent);
        when(rbacAdminModelMapper.toMenu(request)).thenReturn(mapped);
        when(rbacAdminModelMapper.toMenuVO(mapped)).thenAnswer(invocation -> toMenuVO(invocation.getArgument(0)));

        SysMenuAdminVO result = sysMenuAdminService.createMenu(request);

        assertEquals("0,10", mapped.getTreePath());
        assertEquals("0,10", result.getTreePath());
    }

    @Test
    void createMenuShouldThrowWhenTypeInvalid() {
        SysMenuSaveRequest request = menuSaveRequest(0L, "系统管理", "X");

        BusinessException exception = assertThrows(BusinessException.class, () -> sysMenuAdminService.createMenu(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("菜单类型非法", exception.getMessage());
        verify(sysMenuService, never()).save(any(SysMenu.class));
    }

    @Test
    void updateMenuShouldRefreshChildrenTreePathWhenParentChanges() {
        SysMenu existing = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        SysMenu newParent = menu(10L, 0L, "0", "新父级", MenuConstants.TYPE_CATALOG);
        SysMenu child = menu(3L, 2L, "0,1,2", "详情", MenuConstants.TYPE_MENU);

        SysMenuSaveRequest request = menuSaveRequest(10L, "用户管理", MenuConstants.TYPE_MENU);
        request.setRoutePath("/user");

        when(sysMenuService.getById(2L)).thenReturn(existing);
        when(sysMenuService.getById(10L)).thenReturn(newParent);
        when(sysMenuService.lambdaQuery()).thenReturn(childQuery, grandChildQuery);
        when(childQuery.eq(any(SFunction.class), any())).thenReturn(childQuery);
        when(childQuery.list()).thenReturn(List.of(child));
        when(grandChildQuery.eq(any(SFunction.class), any())).thenReturn(grandChildQuery);
        when(grandChildQuery.list()).thenReturn(List.of());
        when(rbacAdminModelMapper.toMenuVO(existing)).thenAnswer(invocation -> toMenuVO(invocation.getArgument(0)));

        SysMenuAdminVO result = sysMenuAdminService.updateMenu(2L, request);

        assertEquals("0,10", existing.getTreePath());
        assertEquals("0,10,2", child.getTreePath());
        verify(sysMenuService).updateById(existing);
        verify(sysMenuService).updateById(child);
        assertEquals("0,10", result.getTreePath());
    }

    @Test
    void updateMenuShouldRejectDescendantAsParent() {
        SysMenu existing = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        SysMenu descendant = menu(5L, 2L, "0,1,2", "详情", MenuConstants.TYPE_MENU);
        SysMenuSaveRequest request = menuSaveRequest(5L, "用户管理", MenuConstants.TYPE_MENU);

        when(sysMenuService.getById(2L)).thenReturn(existing);
        when(sysMenuService.getById(5L)).thenReturn(descendant);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysMenuAdminService.updateMenu(2L, request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("父菜单不能选择当前菜单的子节点", exception.getMessage());
        verify(sysMenuService, never()).updateById(existing);
    }

    @Test
    void deleteMenuShouldRejectWhenChildrenExist() {
        SysMenu existing = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        when(sysMenuService.getById(2L)).thenReturn(existing);
        when(sysMenuService.lambdaQuery()).thenReturn(existsQuery);
        when(existsQuery.eq(any(SFunction.class), any())).thenReturn(existsQuery);
        when(existsQuery.exists()).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysMenuAdminService.deleteMenu(2L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("当前菜单存在子菜单，无法删除", exception.getMessage());
        verify(sysRoleMenuService, never()).removeByMenuId(2L);
        verify(sysMenuService, never()).removeById(2L);
    }

    @Test
    void deleteMenuShouldRemoveRoleMenusAndDeleteMenu() {
        SysMenu existing = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        when(sysMenuService.getById(2L)).thenReturn(existing);
        when(sysMenuService.lambdaQuery()).thenReturn(existsQuery);
        when(existsQuery.eq(any(SFunction.class), any())).thenReturn(existsQuery);
        when(existsQuery.exists()).thenReturn(false);

        sysMenuAdminService.deleteMenu(2L);

        verify(sysRoleMenuService).removeByMenuId(2L);
        verify(sysMenuService).removeById(2L);
    }

    private void mockMenuMappings() {
        lenient().doAnswer(invocation -> {
            SysMenuSaveRequest request = invocation.getArgument(0);
            SysMenu menu = invocation.getArgument(1);
            menu.setParentId(request.getParentId());
            menu.setName(request.getName());
            menu.setType(request.getType());
            menu.setRouteName(request.getRouteName());
            menu.setRoutePath(request.getRoutePath());
            menu.setComponent(request.getComponent());
            menu.setPerm(request.getPerm());
            menu.setAlwaysShow(request.getAlwaysShow() != null ? request.getAlwaysShow() : 0);
            menu.setKeepAlive(request.getKeepAlive() != null ? request.getKeepAlive() : 0);
            menu.setVisible(request.getVisible() != null ? request.getVisible() : 1);
            menu.setSort(request.getSort() != null ? request.getSort() : 0);
            menu.setIcon(request.getIcon());
            menu.setRedirect(request.getRedirect());
            menu.setParams(request.getParams());
            return null;
        }).when(rbacAdminModelMapper).updateMenu(any(SysMenuSaveRequest.class), any(SysMenu.class));
        lenient().when(rbacAdminModelMapper.toMenuVO(any(SysMenu.class))).thenAnswer(invocation -> toMenuVO(invocation.getArgument(0)));
    }

    private void stubSaveMenu() {
        doAnswer(invocation -> {
            SysMenu menu = invocation.getArgument(0);
            if (menu.getId() == null) {
                menu.setId(100L);
            }
            return true;
        }).when(sysMenuService).save(any(SysMenu.class));
    }

    private SysMenuAdminVO toMenuVO(SysMenu menu) {
        SysMenuAdminVO vo = new SysMenuAdminVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setTreePath(menu.getTreePath());
        vo.setName(menu.getName());
        vo.setType(menu.getType());
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private SysMenu menu(Long id, Long parentId, String treePath, String name, String type) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setTreePath(treePath);
        menu.setName(name);
        menu.setType(type);
        return menu;
    }

    private SysMenuSaveRequest menuSaveRequest(Long parentId, String name, String type) {
        SysMenuSaveRequest request = new SysMenuSaveRequest();
        request.setParentId(parentId);
        request.setName(name);
        request.setType(type);
        return request;
    }
}
