package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.RbacAdminModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserSaveRequest;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private LambdaQueryChainWrapper<SysUser> userQuery;

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

    /**
     * Stub the fluent user query chain with lenient matching.
     * MyBatis-Plus lambda query methods take a boolean condition as the first argument,
     * which causes strict Mockito stubbing issues. Lenient stubs avoid false negatives.
     */
    private void stubUserQueryChain() {
        lenient().when(userQuery.like(anyBoolean(), any(SFunction.class), any())).thenReturn(userQuery);
        lenient().when(userQuery.eq(anyBoolean(), any(SFunction.class), any())).thenReturn(userQuery);
        lenient().when(userQuery.eq(any(SFunction.class), any())).thenReturn(userQuery);
        lenient().when(userQuery.ne(anyBoolean(), any(SFunction.class), any())).thenReturn(userQuery);
        lenient().when(userQuery.orderByDesc(any(SFunction.class))).thenReturn(userQuery);
    }

    // ==================== pageUsers ====================

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

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        stubUserQueryChain();
        when(userQuery.page(any())).thenReturn(page);
        when(sysUserRoleService.listRoleIdsByUserId(1L)).thenReturn(List.of(1L, 2L));

        SysUserAdminVO vo = new SysUserAdminVO();
        vo.setId(1L);
        when(rbacAdminModelMapper.toUserVO(user, List.of(1L, 2L))).thenReturn(vo);

        PageResult<SysUserAdminVO> result = sysUserAdminService.pageUsers(query);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getRecords().get(0).getId());
        verify(userQuery).page(any());
    }

    @Test
    void pageUsersShouldReturnEmptyWhenNoData() {
        SysUserPageQuery query = new SysUserPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        Page<SysUser> emptyPage = new Page<>(1, 10, 0);
        emptyPage.setRecords(Collections.emptyList());

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        stubUserQueryChain();
        when(userQuery.page(any())).thenReturn(emptyPage);

        PageResult<SysUserAdminVO> result = sysUserAdminService.pageUsers(query);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== getUser ====================

    @Test
    void getUserShouldReturnUserVOWithRoleIds() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setDeletedFlag(0);

        when(sysUserService.getById(1L)).thenReturn(user);
        when(sysUserRoleService.listRoleIdsByUserId(1L)).thenReturn(List.of(1L, 2L));

        SysUserAdminVO expectedVO = new SysUserAdminVO();
        expectedVO.setId(1L);
        expectedVO.setRoleIds(List.of(1L, 2L));
        when(rbacAdminModelMapper.toUserVO(user, List.of(1L, 2L))).thenReturn(expectedVO);

        SysUserAdminVO result = sysUserAdminService.getUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(List.of(1L, 2L), result.getRoleIds());
    }

    @Test
    void getUserShouldThrowWhenUserNotFound() {
        when(sysUserService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.getUser(999L));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void getUserShouldThrowWhenUserIsDeleted() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(1);

        when(sysUserService.getById(1L)).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.getUser(1L));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== createUser ====================

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

        // Mock validateUserUniqueness: lambdaQuery called 3 times (username, email, phone)
        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        stubUserQueryChain();
        when(userQuery.exists()).thenReturn(false, false, false);

        SysUserAdminVO expectedVO = new SysUserAdminVO();
        expectedVO.setUsername("newuser");
        when(rbacAdminModelMapper.toUserVO(mappedUser, List.of())).thenReturn(expectedVO);

        SysUserAdminVO result = sysUserAdminService.createUser(request);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(rbacAdminModelMapper).updateUser(request, mappedUser);
        verify(sysUserService).save(mappedUser);
        assertEquals("encoded_pwd", mappedUser.getPassword());
    }

    @Test
    void createUserShouldThrowWhenPasswordIsBlank() {
        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("newuser");
        request.setPassword("");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.createUser(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("新增用户时密码不能为空", exception.getMessage());
        verify(sysUserService, never()).save(any());
    }

    @Test
    void createUserShouldThrowWhenUsernameAlreadyExists() {
        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("existinguser");
        request.setPassword("123456");

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        stubUserQueryChain();
        when(userQuery.exists()).thenReturn(true); // username exists

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.createUser(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
        verify(sysUserService, never()).save(any());
    }

    @Test
    void createUserShouldThrowWhenEmailAlreadyExists() {
        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("newuser");
        request.setPassword("123456");
        request.setEmail("taken@example.com");

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        stubUserQueryChain();
        when(userQuery.exists()).thenReturn(false, true); // username OK, email exists

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.createUser(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("邮箱已存在", exception.getMessage());
        verify(sysUserService, never()).save(any());
    }

    @Test
    void createUserShouldThrowWhenPhoneAlreadyExists() {
        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("newuser");
        request.setPassword("123456");
        request.setPhone("13800138000");
        // email is null, so email uniqueness check is short-circuited (skipped)

        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        stubUserQueryChain();
        when(userQuery.exists()).thenReturn(false, true); // username OK, phone exists

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.createUser(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("手机号已存在", exception.getMessage());
        verify(sysUserService, never()).save(any());
    }

    // ==================== updateUser ====================

    @Test
    void updateUserShouldUpdateAndReturnVO() {
        SysUser existingUser = new SysUser();
        existingUser.setId(1L);
        existingUser.setUsername("olduser");
        existingUser.setDeletedFlag(0);

        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("updateduser");
        request.setEmail("updated@example.com");

        when(sysUserService.getById(1L)).thenReturn(existingUser);
        // Mock validateUserUniqueness: 3 lambdaQuery calls for username, email, phone
        when(sysUserService.lambdaQuery()).thenReturn(userQuery);
        stubUserQueryChain();
        when(userQuery.exists()).thenReturn(false, false, false);

        when(sysUserRoleService.listRoleIdsByUserId(1L)).thenReturn(List.of(2L));

        SysUserAdminVO expectedVO = new SysUserAdminVO();
        expectedVO.setId(1L);
        expectedVO.setRoleIds(List.of(2L));
        when(rbacAdminModelMapper.toUserVO(existingUser, List.of(2L))).thenReturn(expectedVO);

        SysUserAdminVO result = sysUserAdminService.updateUser(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(rbacAdminModelMapper).updateUser(request, existingUser);
        verify(sysUserService).updateById(existingUser);
    }

    @Test
    void updateUserShouldThrowWhenUserNotFound() {
        SysUserSaveRequest request = new SysUserSaveRequest();
        request.setUsername("updateduser");

        when(sysUserService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.updateUser(999L, request));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(sysUserService, never()).updateById(any());
    }

    // ==================== updateStatus ====================

    @Test
    void updateStatusShouldSetStatusAndUpdate() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(1L)).thenReturn(user);

        sysUserAdminService.updateStatus(1L, 0);

        assertEquals(0, user.getStatus());
        verify(sysUserService).updateById(user);
    }

    @Test
    void updateStatusShouldThrowWhenUserNotFound() {
        when(sysUserService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.updateStatus(999L, 0));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(sysUserService, never()).updateById(any());
    }

    // ==================== resetPassword ====================

    @Test
    void resetPasswordShouldEncodeAndUpdate() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(1L)).thenReturn(user);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded_new_pwd");

        sysUserAdminService.resetPassword(1L, "newPassword123");

        assertEquals("encoded_new_pwd", user.getPassword());
        verify(sysUserService).updateById(user);
    }

    @Test
    void resetPasswordShouldThrowWhenPasswordIsBlank() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.resetPassword(1L, ""));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("新密码不能为空", exception.getMessage());
        verify(sysUserService, never()).updateById(any());
    }

    @Test
    void resetPasswordShouldThrowWhenPasswordIsNull() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.resetPassword(1L, null));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("新密码不能为空", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void resetPasswordShouldThrowWhenUserNotFound() {
        when(sysUserService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.resetPassword(999L, "newPassword123"));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(passwordEncoder, never()).encode(any());
    }

    // ==================== deleteUser ====================

    @Test
    void deleteUserShouldSoftDeleteAndRemoveRoles() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(1L)).thenReturn(user);

        sysUserAdminService.deleteUser(1L);

        assertEquals(1, user.getDeletedFlag());
        verify(sysUserService).updateById(user);
        verify(sysUserRoleService).removeByUserId(1L);
    }

    @Test
    void deleteUserShouldThrowWhenUserNotFound() {
        when(sysUserService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.deleteUser(999L));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(sysUserService, never()).updateById(any());
        verify(sysUserRoleService, never()).removeByUserId(any());
    }

    @Test
    void deleteUserShouldThrowWhenUserAlreadyDeleted() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(1);

        when(sysUserService.getById(1L)).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.deleteUser(1L));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(sysUserService, never()).updateById(any());
        verify(sysUserRoleService, never()).removeByUserId(any());
    }

    // ==================== listRoleIds ====================

    @Test
    void listRoleIdsShouldReturnRoleIdsForAvailableUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(1L)).thenReturn(user);
        when(sysUserRoleService.listRoleIdsByUserId(1L)).thenReturn(List.of(1L, 2L, 3L));

        List<Long> result = sysUserAdminService.listRoleIds(1L);

        assertEquals(List.of(1L, 2L, 3L), result);
        verify(sysUserRoleService).listRoleIdsByUserId(1L);
    }

    @Test
    void listRoleIdsShouldThrowWhenUserNotFound() {
        when(sysUserService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.listRoleIds(999L));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(sysUserRoleService, never()).listRoleIdsByUserId(any());
    }

    // ==================== assignRoles (existing tests) ====================

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

    @Test
    void assignRolesShouldThrowWhenUserNotFound() {
        when(sysUserService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserAdminService.assignRoles(999L, List.of(1L, 2L)));

        assertEquals(ResultErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(sysUserRoleService, never()).replaceUserRoles(any(), any());
    }

    @Test
    void assignRolesShouldSkipValidationWhenRoleIdsEmpty() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(1L)).thenReturn(user);

        sysUserAdminService.assignRoles(1L, Collections.emptyList());

        verify(sysRoleService, never()).lambdaQuery();
        verify(sysUserRoleService).replaceUserRoles(1L, Collections.emptyList());
    }

    @Test
    void assignRolesShouldSkipValidationWhenRoleIdsNull() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeletedFlag(0);

        when(sysUserService.getById(1L)).thenReturn(user);

        sysUserAdminService.assignRoles(1L, null);

        verify(sysRoleService, never()).lambdaQuery();
        verify(sysUserRoleService).replaceUserRoles(1L, null);
    }
}
