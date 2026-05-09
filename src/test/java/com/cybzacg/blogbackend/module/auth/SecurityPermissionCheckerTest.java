package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityPermissionCheckerTest {
    private final SecurityPermissionChecker checker = new SecurityPermissionChecker();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void wildcardPermissionShouldGrantAnyPermission() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "admin",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_admin"),
                        new SimpleGrantedAuthority(AuthConstants.ALL_PERMISSION)
                )));

        assertTrue(checker.hasPermission("future:module:create"));
        assertTrue(checker.hasAnyPermission("missing:one", "future:module:update"));
        assertTrue(SecurityUtils.hasAuthority("content:article:delete"));
    }

    @Test
    void wildcardPermissionShouldNotGrantArbitraryRoles() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "admin",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_admin"),
                        new SimpleGrantedAuthority(AuthConstants.ALL_PERMISSION)
                )));

        assertTrue(checker.hasRole("admin"));
        assertFalse(checker.hasRole("auditor"));
        assertFalse(checker.hasAnyRole("auditor", "editor"));
    }
}
