package com.cybzacg.blogbackend.core.security;

import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.stereotype.Component;

/**
 * Spring Security 权限校验组件
 *
 * <p>用法示例：</p>
 * <p>{@code @PreAuthorize("@permission.hasPermission('sys:user:list')")}</p>
 * <p>{@code @PreAuthorize("@permission.hasAnyRole('admin','editor')")}</p>
 */
@Component("permission")
public class SecurityPermissionChecker {
    private static final String ROLE_PREFIX = "ROLE_";

    public boolean hasPermission(String permission) {
        return SecurityUtils.isAuthenticated() && SecurityUtils.hasAuthority(permission);
    }

    public boolean hasAnyPermission(String... permissions) {
        return SecurityUtils.isAuthenticated() && SecurityUtils.hasAnyAuthority(permissions);
    }

    public boolean hasRole(String roleCode) {
        return hasAuthority(toRoleAuthority(roleCode));
    }

    public boolean hasAnyRole(String... roleCodes) {
        if (!SecurityUtils.isAuthenticated() || roleCodes == null || roleCodes.length == 0) {
            return false;
        }

        String[] authorities = new String[roleCodes.length];
        for (int i = 0; i < roleCodes.length; i++) {
            authorities[i] = toRoleAuthority(roleCodes[i]);
        }
        return SecurityUtils.hasAnyAuthority(authorities);
    }

    public boolean hasAuthority(String authority) {
        return SecurityUtils.isAuthenticated() && SecurityUtils.hasAuthority(authority);
    }

    public boolean hasAnyAuthority(String... authorities) {
        return SecurityUtils.isAuthenticated() && SecurityUtils.hasAnyAuthority(authorities);
    }

    public boolean isCurrentUser(Long userId) {
        return userId != null && userId.equals(SecurityUtils.getUserId());
    }

    public boolean isCurrentUsername(String username) {
        String currentUsername = SecurityUtils.getUsername();
        return StrUtils.hasText(username) && username.equals(currentUsername);
    }

    private String toRoleAuthority(String roleCode) {
        if (!StrUtils.hasText(roleCode)) {
            return roleCode;
        }
        return roleCode.startsWith(ROLE_PREFIX) ? roleCode : ROLE_PREFIX + roleCode;
    }
}
