package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.domain.SysRoleMenu;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRolePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleSaveRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysMenuRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleMenuRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRoleRepository;
import com.cybzacg.blogbackend.module.auth.service.impl.SysRoleAdminServiceImpl;
import com.cybzacg.blogbackend.module.auth.service.impl.RbacAssociationFactory;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private RbacAdminModelMapper rbacAdminModelMapper;
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
                rbacAdminModelMapper,
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
        when(rbacAdminModelMapper.toRoleVO(role, List.of(10L, 20L))).thenReturn(vo);

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
        when(rbacAdminModelMapper.toRole(request)).thenReturn(mappedRole);
        SysRoleAdminVO expectedVO = new SysRoleAdminVO();
        expectedVO.setName("editor");
        when(rbacAdminModelMapper.toRoleVO(mappedRole, List.of())).thenReturn(expectedVO);

        SysRoleAdminVO result = sysRoleAdminService.createRole(request);

        assertEquals("editor", result.getName());
        verify(sysRoleRepository).save(mappedRole);
    }

    @Test
    void updateRoleShouldUpdateAndReturnVO() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);

        SysRoleSaveRequest request = new SysRoleSaveRequest();
        request.setName("updated");
        request.setCode("UPDATED");

        when(sysRoleRepository.getById(1L)).thenReturn(role);
        when(sysRoleRepository.existsActiveByName("updated", 1L)).thenReturn(false);
        when(sysRoleRepository.existsActiveByCode("UPDATED", 1L)).thenReturn(false);
        when(sysRoleMenuRepository.findMenuIdsByRoleId(1L)).thenReturn(List.of(10L));
        SysRoleAdminVO expectedVO = new SysRoleAdminVO();
        expectedVO.setId(1L);
        when(rbacAdminModelMapper.toRoleVO(role, List.of(10L))).thenReturn(expectedVO);

        SysRoleAdminVO result = sysRoleAdminService.updateRole(1L, request);

        assertEquals(1L, result.getId());
        verify(sysRoleRepository).updateById(role);
    }

    @Test
    void deleteRoleShouldRemoveRelationsAndDeleteRole() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(1L)).thenReturn(role);

        sysRoleAdminService.deleteRole(1L);

        verify(sysRoleMenuRepository).deleteByRoleId(1L);
        verify(sysUserRoleRepository).deleteByRoleId(1L);
        verify(sysRoleRepository).removeById(1L);
    }

    @Test
    void assignMenusShouldReplaceRoleMenusWhenAllMenusExist() {
        SysRole role = new SysRole();
        role.setId(8L);
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(8L)).thenReturn(role);
        when(sysMenuRepository.countByIds(List.of(3L, 5L))).thenReturn(2L);
        when(sysRoleMenuRepository.saveBatch(anyCollection())).thenReturn(true);

        sysRoleAdminService.assignMenus(8L, List.of(3L, 3L, 5L));

        verify(sysRoleMenuRepository).deleteByRoleId(8L);
        ArgumentCaptor<Collection<SysRoleMenu>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(sysRoleMenuRepository).saveBatch(captor.capture());
        assertEquals(List.of(3L, 5L), captor.getValue().stream().map(SysRoleMenu::getMenuId).toList());
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
        role.setId(1L);
        role.setIsDeleted(0);

        when(sysRoleRepository.getById(1L)).thenReturn(role);

        sysRoleAdminService.assignMenus(1L, Collections.emptyList());

        verify(sysRoleMenuRepository).deleteByRoleId(1L);
        verify(sysRoleMenuRepository, never()).saveBatch(anyCollection());
    }
}
