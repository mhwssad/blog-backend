package com.cybzacg.blogbackend.module.auth.rbac;

import com.cybzacg.blogbackend.common.constant.MenuConstants;
import com.cybzacg.blogbackend.domain.auth.SysMenu;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.rbac.convert.RbacAdminModelConvert;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysMenuSaveRequest;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysMenuRepository;
import com.cybzacg.blogbackend.module.auth.rbac.repository.SysRoleMenuRepository;
import com.cybzacg.blogbackend.module.auth.rbac.service.impl.SysMenuAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysMenuAdminServiceImplTest {
    @Mock
    private SysMenuRepository sysMenuRepository;
    @Mock
    private SysRoleMenuRepository sysRoleMenuRepository;
    @Mock
    private RbacAdminModelConvert rbacAdminModelConvert;

    private SysMenuAdminServiceImpl sysMenuAdminService;

    @BeforeEach
    void setUp() {
        sysMenuAdminService = new SysMenuAdminServiceImpl(sysMenuRepository, sysRoleMenuRepository, rbacAdminModelConvert);
    }

    @Test
    void listMenuTreeShouldBuildHierarchicalTree() {
        SysMenu root = menu(1L, 0L, "0", "系统管理", MenuConstants.TYPE_CATALOG);
        SysMenu child = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        SysMenu button = menu(3L, 2L, "0,1,2", "新增", MenuConstants.TYPE_BUTTON);

        when(sysMenuRepository.findAllOrdered()).thenReturn(List.of(root, child, button));
        stubMenuVoMapping();

        List<SysMenuAdminVO> result = sysMenuAdminService.listMenuTree();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(2L, result.get(0).getChildren().get(0).getId());
    }

    @Test
    void createMenuShouldSaveRootMenuWithRootTreePath() {
        SysMenuSaveRequest request = menuSaveRequest(0L, "系统管理", MenuConstants.TYPE_CATALOG);
        SysMenu mapped = new SysMenu();
        when(rbacAdminModelConvert.toMenu(request)).thenReturn(mapped);
        when(sysMenuRepository.save(mapped)).thenAnswer(invocation -> {
            mapped.setId(100L);
            return true;
        });
        stubMenuVoMapping();

        SysMenuAdminVO result = sysMenuAdminService.createMenu(request);

        assertEquals(100L, result.getId());
        assertEquals("0", mapped.getTreePath());
        verify(sysMenuRepository).save(mapped);
    }

    @Test
    void updateMenuShouldRefreshChildrenTreePathWhenParentChanges() {
        SysMenu existing = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        SysMenu newParent = menu(10L, 0L, "0", "新父级", MenuConstants.TYPE_CATALOG);
        SysMenu child = menu(3L, 2L, "0,1,2", "详情", MenuConstants.TYPE_MENU);

        SysMenuSaveRequest request = menuSaveRequest(10L, "用户管理", MenuConstants.TYPE_MENU);

        when(sysMenuRepository.getById(2L)).thenReturn(existing);
        when(sysMenuRepository.getById(10L)).thenReturn(newParent);
        when(sysMenuRepository.findByParentId(2L)).thenReturn(List.of(child));
        when(sysMenuRepository.findByParentId(3L)).thenReturn(List.of());
        stubMenuUpdateMapping();
        stubMenuVoMapping();

        SysMenuAdminVO result = sysMenuAdminService.updateMenu(2L, request);

        assertEquals("0,10", existing.getTreePath());
        assertEquals("0,10,2", child.getTreePath());
        assertEquals("0,10", result.getTreePath());
    }

    @Test
    void deleteMenuShouldRejectWhenChildrenExist() {
        SysMenu existing = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        when(sysMenuRepository.getById(2L)).thenReturn(existing);
        when(sysMenuRepository.existsByParentId(2L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysMenuAdminService.deleteMenu(2L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(sysRoleMenuRepository, never()).deleteByMenuId(2L);
    }

    @Test
    void deleteMenuShouldRemoveRoleMenusAndDeleteMenu() {
        SysMenu existing = menu(2L, 1L, "0,1", "用户管理", MenuConstants.TYPE_MENU);
        when(sysMenuRepository.getById(2L)).thenReturn(existing);
        when(sysMenuRepository.existsByParentId(2L)).thenReturn(false);

        sysMenuAdminService.deleteMenu(2L);

        verify(sysRoleMenuRepository).deleteByMenuId(2L);
        verify(sysMenuRepository).removeById(2L);
    }

    private void stubMenuUpdateMapping() {
        doAnswer(invocation -> {
            SysMenuSaveRequest request = invocation.getArgument(0);
            SysMenu menu = invocation.getArgument(1);
            menu.setParentId(request.getParentId());
            menu.setName(request.getName());
            menu.setType(request.getType());
            return null;
        }).when(rbacAdminModelConvert).updateMenu(any(SysMenuSaveRequest.class), any(SysMenu.class));
    }

    private void stubMenuVoMapping() {
        when(rbacAdminModelConvert.toMenuVO(any(SysMenu.class))).thenAnswer(invocation -> toMenuVO(invocation.getArgument(0)));
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
