package com.cybzacg.blogbackend.module.auth.account.model;

import com.cybzacg.blogbackend.domain.auth.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
@Schema(description = "认证用户详情")
public class AuthUserDetails implements UserDetails {
    @Schema(description = "用户ID")
    private final Long userId;
    @Schema(description = "用户名")
    private final String username;
    @Schema(description = "密码")
    private final String password;
    @Schema(description = "昵称")
    private final String nickname;
    @Schema(description = "状态")
    private final Integer status;
    @Schema(description = "角色编码列表")
    private final List<String> roleCodes;
    @Schema(description = "权限标识列表")
    private final List<String> permissions;
    @Schema(description = "授权集合")
    private final Collection<? extends GrantedAuthority> authorities;

    public static AuthUserDetails of(SysUser user,
                                     List<String> roleCodes,
                                     List<String> permissions,
                                     Collection<? extends GrantedAuthority> authorities) {
        return AuthUserDetails.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .roleCodes(roleCodes)
                .permissions(permissions)
                .authorities(authorities)
                .build();
    }

    public AuthUserPrincipal toPrincipal() {
        return new AuthUserPrincipal(userId, username);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }
}
