package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.cybzacg.blogbackend.domain.SysUserRole;
import com.cybzacg.blogbackend.module.auth.service.impl.SysUserRoleServiceImpl;
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
class SysUserRoleServiceImplTest {
    @Mock
    private LambdaQueryChainWrapper<SysUserRole> userRoleQuery;
    @Mock
    private LambdaUpdateChainWrapper<SysUserRole> userRoleUpdate;

    private SysUserRoleServiceImpl sysUserRoleService;

    @BeforeEach
    void setUp() {
        sysUserRoleService = spy(new SysUserRoleServiceImpl());
    }

    @Test
    void listRoleIdsByUserIdShouldReturnDistinctRoleIds() {
        when(userRoleQuery.eq(any(SFunction.class), any())).thenReturn(userRoleQuery);
        when(userRoleQuery.list()).thenReturn(List.of(userRole(7L, 1L), userRole(7L, 1L), userRole(7L, 3L)));
        doReturn(userRoleQuery).when(sysUserRoleService).lambdaQuery();

        List<Long> result = sysUserRoleService.listRoleIdsByUserId(7L);

        assertEquals(List.of(1L, 3L), result);
    }

    @Test
    void replaceUserRolesShouldRebuildDistinctNonNullRelations() {
        when(userRoleUpdate.eq(any(SFunction.class), any())).thenReturn(userRoleUpdate);
        when(userRoleUpdate.remove()).thenReturn(true);
        doReturn(userRoleUpdate).when(sysUserRoleService).lambdaUpdate();
        doReturn(true).when(sysUserRoleService).saveBatch(any(Collection.class));

        sysUserRoleService.replaceUserRoles(6L, Arrays.asList(2L, null, 2L, 4L));

        ArgumentCaptor<Collection<SysUserRole>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(sysUserRoleService).saveBatch(captor.capture());
        List<SysUserRole> relations = captor.getValue().stream().toList();
        assertEquals(2, relations.size());
        assertEquals(List.of(2L, 4L), relations.stream().map(SysUserRole::getRoleId).toList());
        assertTrue(relations.stream().allMatch(item -> Long.valueOf(6L).equals(item.getUserId())));
    }

    @Test
    void replaceUserRolesShouldSkipSaveWhenRoleIdsEmpty() {
        when(userRoleUpdate.eq(any(SFunction.class), any())).thenReturn(userRoleUpdate);
        when(userRoleUpdate.remove()).thenReturn(true);
        doReturn(userRoleUpdate).when(sysUserRoleService).lambdaUpdate();

        sysUserRoleService.replaceUserRoles(6L, List.of());

        verify(sysUserRoleService, never()).saveBatch(any(Collection.class));
    }

    @Test
    void removeByUserIdShouldDeleteByUserId() {
        when(userRoleUpdate.eq(any(SFunction.class), any())).thenReturn(userRoleUpdate);
        when(userRoleUpdate.remove()).thenReturn(true);
        doReturn(userRoleUpdate).when(sysUserRoleService).lambdaUpdate();

        sysUserRoleService.removeByUserId(6L);

        verify(userRoleUpdate).remove();
    }

    @Test
    void removeByRoleIdShouldDeleteByRoleId() {
        when(userRoleUpdate.eq(any(SFunction.class), any())).thenReturn(userRoleUpdate);
        when(userRoleUpdate.remove()).thenReturn(true);
        doReturn(userRoleUpdate).when(sysUserRoleService).lambdaUpdate();

        sysUserRoleService.removeByRoleId(4L);

        verify(userRoleUpdate).remove();
    }

    private SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        return relation;
    }
}
