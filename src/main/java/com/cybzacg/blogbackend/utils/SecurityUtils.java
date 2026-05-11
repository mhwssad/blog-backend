package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security 上下文快捷工具类
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static Authentication requireAuthentication() {
        Authentication authentication = getAuthentication();
        ExceptionThrowerCore.throwBusinessIf(!isAuthenticated(authentication), ResultErrorCode.LOGIN_REQUIRED);
        return authentication;
    }

    public static boolean isAuthenticated() {
        return isAuthenticated(getAuthentication());
    }

    public static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static Object getPrincipal() {
        Authentication authentication = getAuthentication();
        return authentication != null ? authentication.getPrincipal() : null;
    }

    public static Long getUserId() {
        return getUserId(getAuthentication());
    }

    public static Long requireUserId() {
        Long userId = getUserId();
        return ExceptionThrowerCore.requireNonNull(userId, ResultErrorCode.LOGIN_REQUIRED);
    }

    public static Long getUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return extractUserId(authentication.getPrincipal(), authentication.getDetails());
    }

    public static String getUsername() {
        return getUsername(getAuthentication());
    }

    public static String getUsername(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof AuthUserPrincipal userPrincipal) {
            return userPrincipal.getUsername();
        }
        if (principal instanceof Principal principalInfo && StrUtils.hasText(principalInfo.getName())) {
            return principalInfo.getName();
        }
        return StrUtils.hasText(authentication.getName()) ? authentication.getName() : null;
    }

    public static Set<String> getAuthoritySet() {
        return getAuthoritySet(getAuthentication());
    }

    public static Set<String> getAuthoritySet(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Collections.emptySet();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StrUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean hasAuthority(String authority) {
        if (!StrUtils.hasText(authority)) {
            return false;
        }
        Set<String> currentAuthorities = getAuthoritySet();
        if (currentAuthorities.contains(authority)) {
            return true;
        }
        return currentAuthorities.stream().anyMatch(granted -> matchesPermission(authority, granted));
    }

    public static boolean hasAnyAuthority(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            return false;
        }
        for (String authority : authorities) {
            if (hasAuthority(authority)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通配符权限段匹配。按 {@code :} 分段后，授予权限中 {@code *} 段匹配任意值。
     * 例如 {@code *:*:*} 匹配 {@code sys:user:query}，{@code sys:*:*} 匹配 {@code sys:role:create}。
     */
    private static boolean matchesPermission(String required, String granted) {
        if (!granted.contains("*")) {
            return false;
        }
        String[] requiredParts = required.split(":");
        String[] grantedParts = granted.split(":");
        if (requiredParts.length != grantedParts.length) {
            return false;
        }
        for (int i = 0; i < requiredParts.length; i++) {
            if (!"*".equals(grantedParts[i]) && !requiredParts[i].equals(grantedParts[i])) {
                return false;
            }
        }
        return true;
    }

    private static Long extractUserId(Object... candidates) {
        if (candidates == null) {
            return null;
        }

        for (Object candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (candidate instanceof AuthUserDetails userDetails) {
                return userDetails.getUserId();
            }
            if (candidate instanceof AuthUserPrincipal userPrincipal) {
                return userPrincipal.getUserId();
            }
            if (candidate instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }
}
