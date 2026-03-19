package com.cybzacg.blogbackend.module.auth.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * 邮箱验证码认证 Token
 */
public class EmailCodeAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;
    private Object credentials;
    private final boolean authenticated;

    private EmailCodeAuthenticationToken(Object principal,
                                         Object credentials,
                                         Collection<? extends GrantedAuthority> authorities,
                                         boolean authenticated) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.authenticated = authenticated;
        super.setAuthenticated(authenticated);
    }

    public static EmailCodeAuthenticationToken unauthenticated(String email, String code) {
        return new EmailCodeAuthenticationToken(email, code, null, false);
    }

    public static EmailCodeAuthenticationToken authenticated(Object principal,
                                                             Collection<? extends GrantedAuthority> authorities) {
        return new EmailCodeAuthenticationToken(principal, null, authorities == null ? List.of() : authorities, true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated && !authenticated) {
            throw new IllegalArgumentException("请使用已认证构造方法创建认证 Token");
        }
        super.setAuthenticated(isAuthenticated);
    }
}
