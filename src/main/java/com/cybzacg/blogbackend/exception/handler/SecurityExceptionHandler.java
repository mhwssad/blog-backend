package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security 异常处理器。<p>统一处理认证失败、访问拒绝、CSRF 令牌异常和 Remember-Me Cookie 异常，映射为语义化的错误码。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler extends BaseExceptionHandler {
    /**
     * 处理访问拒绝异常（权限不足）。
     *
     * @param e 访问拒绝异常
     * @return 权限不足错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Object> handleAccessDenied(AccessDeniedException e) {
        log.warn("访问拒绝异常 [TraceID: {}] - {}", getTraceId(), e.getMessage());
        return buildSecurityError(ResultErrorCode.FORBIDDEN, e);
    }

    /**
     * 处理认证异常，根据子类型映射为具体的认证错误码。
     *
     * @param e 认证异常
     * @return 对应认证错误码的统一响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Object> handleAuthentication(AuthenticationException e) {
        log.warn("认证失败异常 [TraceID: {}] - {}", getTraceId(), e.getMessage());

        if (e instanceof BadCredentialsException) {
            return buildSecurityError(ResultErrorCode.INVALID_CREDENTIALS, e);
        }
        if (e instanceof InsufficientAuthenticationException) {
            return buildSecurityError(ResultErrorCode.LOGIN_REQUIRED, e);
        }
        if (e instanceof AccountExpiredException) {
            return buildSecurityError(ResultErrorCode.ACCOUNT_EXPIRED, e);
        }
        if (e instanceof LockedException) {
            return buildSecurityError(ResultErrorCode.ACCOUNT_LOCKED, e);
        }
        if (e instanceof DisabledException) {
            return buildSecurityError(ResultErrorCode.ACCOUNT_DISABLED, e);
        }
        if (e instanceof CredentialsExpiredException) {
            return buildSecurityError(ResultErrorCode.CREDENTIALS_EXPIRED, e);
        }
        if (e instanceof RememberMeAuthenticationException) {
            return buildSecurityError(ResultErrorCode.REMEMBER_ME_AUTH_FAILED, e);
        }
        if (e instanceof SessionAuthenticationException) {
            return buildSecurityError(ResultErrorCode.SESSION_EXPIRED, e);
        }

        return buildSecurityError(ResultErrorCode.AUTH_FAILED, e);
    }

    /**
     * 处理用户名或密码错误异常。
     *
     * @param e 凭据错误异常
     * @return 凭据无效错误响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Object> handleBadCredentials(BadCredentialsException e) {
        log.warn("用户名或密码错误 [TraceID: {}] - {}", getTraceId(), e.getMessage());
        return buildSecurityError(ResultErrorCode.INVALID_CREDENTIALS, e);
    }

    /**
     * 处理用户名不存在异常，生产环境统一返回凭据无效以防止用户枚举。
     *
     * @param e 用户名未找到异常
     * @return 用户不存在或凭据无效错误响应
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public Result<Object> handleUsernameNotFound(UsernameNotFoundException e) {
        log.warn("用户名不存在异常 [TraceID: {}] - {}", getTraceId(), e.getMessage());

        if (isProductionProfile()) {
            return buildErrorResult(ResultErrorCode.INVALID_CREDENTIALS);
        }
        return buildErrorResult(ResultErrorCode.USER_NOT_FOUND, e.getMessage());
    }

    /**
     * 处理 CSRF 令牌异常，区分缺失和无效场景。
     *
     * @param e CSRF 异常
     * @return 对应 CSRF 错误码的统一响应
     */
    @ExceptionHandler({CsrfException.class, InvalidCsrfTokenException.class, MissingCsrfTokenException.class})
    public Result<Object> handleCsrf(CsrfException e) {
        log.warn("CSRF令牌验证失败 [TraceID: {}] - {}", getTraceId(), e.getMessage());

        ResultErrorCode resultCode = ResultErrorCode.CSRF_TOKEN_VALIDATION_FAILED;
        if (e instanceof MissingCsrfTokenException) {
            resultCode = ResultErrorCode.CSRF_TOKEN_MISSING;
        } else if (e instanceof InvalidCsrfTokenException) {
            resultCode = ResultErrorCode.CSRF_TOKEN_INVALID;
        }

        return buildSecurityError(resultCode, e);
    }

    /**
     * 处理 Remember-Me Cookie 异常，检测 Cookie 盗用时记录安全警告。
     *
     * @param e Cookie 异常
     * @return Cookie 无效或盗用错误响应
     */
    @ExceptionHandler({CookieTheftException.class, InvalidCookieException.class})
    public Result<Object> handleRememberMeCookie(Exception e) {
        log.error("Remember-Me Cookie异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);

        ResultErrorCode resultCode = ResultErrorCode.INVALID_REMEMBER_ME_COOKIE;
        if (e instanceof CookieTheftException) {
            resultCode = ResultErrorCode.COOKIE_THEFT_DETECTED;
            log.error("安全警告：检测到Cookie盗用尝试 [TraceID: {}]", getTraceId());
        }

        return buildSecurityError(resultCode, e);
    }

    private Result<Object> buildSecurityError(ResultErrorCode resultCode, Exception e) {
        return buildErrorResult(resultCode, nonProductionMessage(e));
    }
}
