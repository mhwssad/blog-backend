package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.SysUserRole;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserSaveRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRoleRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private SysUserAdminServiceImpl sysUserAdminService;

    @BeforeEach
    void setUp() {
        sysUserAdminService = new SysUserAdminServiceImpl(
                sysUserRepository,
                sysRoleRepository,
                sysUserRoleRepository,
                passwordEncoder,
                rbacAdminModelMapper
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
        when(sysRoleRepository.countActiveByIds(List.of(2L, 4L))).thenReturn(2L);
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
}
