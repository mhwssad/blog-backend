package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.service.AuthUserDetailsService;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 认证用户详情服务实现。
 *
 * <p>负责根据用户名装配认证用户信息，并聚合角色与权限生成 Spring Security 权限集。
 */
@Service
@RequiredArgsConstructor
public class AuthUserDetailsServiceImpl implements AuthUserDetailsService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;

    @Override
    public AuthUserDetails loadAuthUserByUsername(String username) {
        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<String> roleCodes = sysRoleService.listRoleCodesByUserId(user.getId());
        List<String> permissions = sysMenuService.listPermissionsByUserId(user.getId());
        List<GrantedAuthority> authorities = buildAuthorities(roleCodes, permissions);
        return AuthUserDetails.of(user, roleCodes, permissions, authorities);
    }

    @Override
    public AuthUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadAuthUserByUsername(username);
    }

    /**
     * 将角色编码和菜单权限合并为去重后的授权集合，供认证上下文直接使用。
     */
    private List<GrantedAuthority> buildAuthorities(List<String> roleCodes, List<String> permissions) {
        Set<String> authorityValues = new LinkedHashSet<>();
        if (roleCodes != null) {
            roleCodes.stream()
                    .filter(StringUtils::hasText)
                    .map(this::toRoleAuthority)
                    .forEach(authorityValues::add);
        }
        if (permissions != null) {
            permissions.stream()
                    .filter(StringUtils::hasText)
                    .forEach(authorityValues::add);
        }
        return authorityValues.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    /**
     * 将角色编码统一转换为 Spring Security 约定的 ROLE_ 前缀格式。
     */
    private String toRoleAuthority(String roleCode) {
        return roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;
    }
}
