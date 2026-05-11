package com.cybzacg.blogbackend.module.auth.account.service.impl;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.dto.domain.auth.SysMenu;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysMenuRepository;
import com.cybzacg.blogbackend.dto.repository.auth.rbac.SysRoleRepository;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.account.service.AuthUserDetailsService;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysMenuRepository sysMenuRepository;

    /**
     * 根据用户名加载认证详情，聚合角色编码与菜单权限。
     *
     * @param username 用户名
     * @return 包含角色和权限的认证用户详情
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public AuthUserDetails loadAuthUserByUsername(String username) {
        SysUser user = sysUserRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        List<String> roleCodes = sysRoleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = buildPermissions(roleCodes, user.getId());
        List<GrantedAuthority> authorities = buildAuthorities(roleCodes, permissions);
        return AuthUserDetails.of(user, roleCodes, permissions, authorities);
    }

    /**
     * Spring Security UserDetailsService 契约方法，委托给 {@link #loadAuthUserByUsername}。
     */
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
                    .filter(StrUtils::hasText)
                    .map(this::toRoleAuthority)
                    .forEach(authorityValues::add);
        }
        if (permissions != null) {
            permissions.stream()
                    .filter(StrUtils::hasText)
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

    /**
     * 超级管理员持有权限通配符，后续新增权限无需再补角色菜单授权即可通过鉴权。
     */
    private List<String> buildPermissions(List<String> roleCodes, Long userId) {
        Set<String> permissionValues = new LinkedHashSet<>();
        if (hasSuperAdminRole(roleCodes)) {
            permissionValues.addAll(loadAllPermissions());
            permissionValues.add(AuthConstants.ALL_PERMISSION);
        } else {
            List<String> permissions = sysMenuRepository.findPermissionsByUserId(userId);
            if (permissions != null) {
                permissions.stream()
                        .filter(StrUtils::hasText)
                        .forEach(permissionValues::add);
            }
        }
        return permissionValues.stream().toList();
    }

    private boolean hasSuperAdminRole(List<String> roleCodes) {
        if (roleCodes == null) {
            return false;
        }
        return roleCodes.stream()
                .filter(StrUtils::hasText)
                .map(this::normalizeRoleCode)
                .anyMatch(AuthConstants.SUPER_ADMIN_ROLE_CODE::equals);
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode.startsWith("ROLE_") ? roleCode.substring("ROLE_".length()) : roleCode;
    }

    /**
     * 读取当前系统全部菜单权限，用于超级管理员直接构建完整授权集。
     */
    private List<String> loadAllPermissions() {
        List<SysMenu> menus = sysMenuRepository.findAllOrdered();
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        return menus.stream()
                .map(SysMenu::getPerm)
                .filter(StrUtils::hasText)
                .distinct()
                .toList();
    }
}
