package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserDetails;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.impl.AuthUserDetailsServiceImpl;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysMenuRepository;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserDetailsServiceImplTest {
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private SysMenuRepository sysMenuRepository;

    private AuthUserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthUserDetailsServiceImpl(sysUserRepository, sysRoleRepository, sysMenuRepository);
    }

    @Test
    void loadAuthUserByUsernameShouldAssembleAuthorities() {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("demo");
        user.setPassword("secret");
        user.setStatus(1);

        // "admin" 匹配超级管理员角色，走超级管理员分支，仅加载通配符权限
        when(sysUserRepository.findByUsername("demo")).thenReturn(user);
        when(sysRoleRepository.findRoleCodesByUserId(7L)).thenReturn(List.of("admin", "ROLE_root", " "));

        AuthUserDetails details = service.loadAuthUserByUsername("demo");

        assertEquals(7L, details.getUserId());
        assertEquals("demo", details.getUsername());
        assertEquals("secret", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals("ROLE_admin")));
        assertTrue(details.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals("ROLE_root")));
        assertTrue(details.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals("*:*:*")));
        // 超级管理员不加载菜单权限
        verify(sysMenuRepository, never()).findPermissionsByUserId(anyLong());
        verify(sysUserRepository).findByUsername("demo");
    }

    @Test
    void loadAuthUserByUsernameShouldThrowWhenUserMissing() {
        when(sysUserRepository.findByUsername("missing")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadAuthUserByUsername("missing"));
        verify(sysRoleRepository, never()).findRoleCodesByUserId(anyLong());
        verify(sysMenuRepository, never()).findPermissionsByUserId(anyLong());
    }
}
