package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.cybzacg.blogbackend.domain.SysRoleMenu;
import com.cybzacg.blogbackend.module.auth.service.impl.SysRoleMenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRoleMenuServiceImplTest {
    @Mock
    private LambdaQueryChainWrapper<SysRoleMenu> roleMenuQuery;
    @Mock
    private LambdaUpdateChainWrapper<SysRoleMenu> roleMenuUpdate;

    private SysRoleMenuServiceImpl sysRoleMenuService;

    @BeforeEach
    void setUp() {
        sysRoleMenuService = spy(new SysRoleMenuServiceImpl());
    }

    @Test
    void listMenuIdsByRoleIdShouldReturnDistinctMenuIds() {
        when(roleMenuQuery.eq(any(SFunction.class), any())).thenReturn(roleMenuQuery);
        when(roleMenuQuery.list()).thenReturn(List.of(roleMenu(1L, 10L), roleMenu(1L, 10L), roleMenu(1L, 20L)));
        doReturn(roleMenuQuery).when(sysRoleMenuService).lambdaQuery();

        List<Long> result = sysRoleMenuService.listMenuIdsByRoleId(1L);

        assertEquals(List.of(10L, 20L), result);
    }

    @Test
    void replaceRoleMenusShouldRebuildDistinctNonNullRelations() {
        when(roleMenuUpdate.eq(any(SFunction.class), any())).thenReturn(roleMenuUpdate);
        when(roleMenuUpdate.remove()).thenReturn(true);
        doReturn(roleMenuUpdate).when(sysRoleMenuService).lambdaUpdate();
        doReturn(true).when(sysRoleMenuService).saveBatch(any(Collection.class));

        sysRoleMenuService.replaceRoleMenus(8L, Arrays.asList(3L, null, 3L, 5L));

        ArgumentCaptor<Collection<SysRoleMenu>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(sysRoleMenuService).saveBatch(captor.capture());
        List<SysRoleMenu> relations = captor.getValue().stream().toList();
        assertEquals(2, relations.size());
        assertEquals(List.of(3L, 5L), relations.stream().map(SysRoleMenu::getMenuId).toList());
        assertTrue(relations.stream().allMatch(item -> Long.valueOf(8L).equals(item.getRoleId())));
    }

    @Test
    void replaceRoleMenusShouldSkipSaveWhenMenuIdsEmpty() {
        when(roleMenuUpdate.eq(any(SFunction.class), any())).thenReturn(roleMenuUpdate);
        when(roleMenuUpdate.remove()).thenReturn(true);
        doReturn(roleMenuUpdate).when(sysRoleMenuService).lambdaUpdate();

        sysRoleMenuService.replaceRoleMenus(8L, List.of());

        verify(sysRoleMenuService, never()).saveBatch(any(Collection.class));
    }

    @Test
    void removeByRoleIdShouldDeleteByRoleId() {
        when(roleMenuUpdate.eq(any(SFunction.class), any())).thenReturn(roleMenuUpdate);
        when(roleMenuUpdate.remove()).thenReturn(true);
        doReturn(roleMenuUpdate).when(sysRoleMenuService).lambdaUpdate();

        sysRoleMenuService.removeByRoleId(9L);

        verify(roleMenuUpdate).remove();
    }

    @Test
    void removeByMenuIdShouldDeleteByMenuId() {
        when(roleMenuUpdate.eq(any(SFunction.class), any())).thenReturn(roleMenuUpdate);
        when(roleMenuUpdate.remove()).thenReturn(true);
        doReturn(roleMenuUpdate).when(sysRoleMenuService).lambdaUpdate();

        sysRoleMenuService.removeByMenuId(12L);

        verify(roleMenuUpdate).remove();
    }

    private SysRoleMenu roleMenu(Long roleId, Long menuId) {
        SysRoleMenu relation = new SysRoleMenu();
        relation.setRoleId(roleId);
        relation.setMenuId(menuId);
        return relation;
    }
}
