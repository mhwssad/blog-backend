package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserPrincipal;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security 上下文快捷工具类。
 * <p>
 * 提供从 Spring Security {@link SecurityContextHolder} 中提取当前用户信息、
 * 判断认证状态以及检查权限等快捷方法。
 */
public final class SecurityUtils {

    /** 隐藏构造器，工具类禁止实例化。 */
    private SecurityUtils() {}

    /**
     * 获取当前线程绑定的 {@link Authentication}，未认证时返回 {@code null}。
     *
     * @return 当前认证对象，可能为 {@code null}
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前 {@link Authentication}，若未认证则抛出 {@code LOGIN_REQUIRED} 业务异常。
     *
     * @return 当前已认证的 {@link Authentication}
     */
    public static Authentication requireAuthentication() {
        Authentication authentication = getAuthentication();
        ExceptionThrowerCore.throwBusinessIf(
            !isAuthenticated(authentication),
            ResultErrorCode.LOGIN_REQUIRED
        );
        return authentication;
    }

    /**
     * 判断当前用户是否已认证（非匿名）。
     *
     * @return 已认证返回 {@code true}，否则 {@code false}
     */
    public static boolean isAuthenticated() {
        return isAuthenticated(getAuthentication());
    }

    /**
     * 判断给定 {@link Authentication} 是否代表已认证用户（排除匿名令牌）。
     *
     * @param authentication 待判断的认证对象
     * @return 已认证且非匿名返回 {@code true}，否则 {@code false}
     */
    public static boolean isAuthenticated(Authentication authentication) {
        return (
            authentication != null &&
            authentication.isAuthenticated() &&
            !(authentication instanceof AnonymousAuthenticationToken)
        );
    }

    /**
     * 获取当前认证的主体对象。
     *
     * @return 当前 principal，未认证时返回 {@code null}
     */
    public static Object getPrincipal() {
        Authentication authentication = getAuthentication();
        return authentication != null ? authentication.getPrincipal() : null;
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID，未认证时返回 {@code null}
     */
    public static Long getUserId() {
        return getUserId(getAuthentication());
    }

    /**
     * 获取当前登录用户 ID，未认证时抛出 {@code LOGIN_REQUIRED} 业务异常。
     *
     * @return 当前用户 ID
     */
    public static Long requireUserId() {
        Long userId = getUserId();
        return ExceptionThrowerCore.requireNonNull(
            userId,
            ResultErrorCode.LOGIN_REQUIRED
        );
    }

    /**
     * 从给定 {@link Authentication} 中提取用户 ID。
     *
     * @param authentication 认证对象，可以为 {@code null}
     * @return 用户 ID，无法提取时返回 {@code null}
     */
    public static Long getUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return extractUserId(
            authentication.getPrincipal(),
            authentication.getDetails()
        );
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 用户名，未认证时返回 {@code null}
     */
    public static String getUsername() {
        return getUsername(getAuthentication());
    }

    /**
     * 从给定 {@link Authentication} 中提取用户名。
     *
     * @param authentication 认证对象，可以为 {@code null}
     * @return 用户名，无法提取时返回 {@code null}
     */
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
        if (
            principal instanceof Principal principalInfo &&
            StrUtils.hasText(principalInfo.getName())
        ) {
            return principalInfo.getName();
        }
        return StrUtils.hasText(authentication.getName())
            ? authentication.getName()
            : null;
    }

    /**
     * 获取当前用户拥有的权限标识集合。
     *
     * @return 不可变权限集合，未认证时返回空集合
     */
    public static Set<String> getAuthoritySet() {
        return getAuthoritySet(getAuthentication());
    }

    /**
     * 从给定 {@link Authentication} 中提取权限标识集合。
     *
     * @param authentication 认证对象，可以为 {@code null}
     * @return 不可变权限集合，认证为空或无权限时返回空集合
     */
    public static Set<String> getAuthoritySet(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Collections.emptySet();
        }

        return authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .filter(StrUtils::hasText)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 判断当前用户是否拥有指定权限，支持通配符匹配。
     *
     * @param authority 需要校验的权限标识
     * @return 拥有该权限返回 {@code true}，否则 {@code false}
     */
    public static boolean hasAuthority(String authority) {
        if (!StrUtils.hasText(authority)) {
            return false;
        }
        Set<String> currentAuthorities = getAuthoritySet();
        if (currentAuthorities.contains(authority)) {
            return true;
        }
        return currentAuthorities
            .stream()
            .anyMatch(granted -> matchesPermission(authority, granted));
    }

    /**
     * 判断当前用户是否拥有给定权限中的任意一个，支持通配符匹配。
     *
     * @param authorities 需要校验的权限标识可变参数
     * @return 拥有任一权限返回 {@code true}，否则 {@code false}
     */
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
            if (
                !"*".equals(grantedParts[i]) &&
                !requiredParts[i].equals(grantedParts[i])
            ) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从候选对象数组中提取用户 ID，依次尝试 {@link AuthUserDetails}、{@link AuthUserPrincipal} 和 {@link Number}。
     *
     * @param candidates 候选对象可变参数
     * @return 提取到的用户 ID，无法提取时返回 {@code null}
     */
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
