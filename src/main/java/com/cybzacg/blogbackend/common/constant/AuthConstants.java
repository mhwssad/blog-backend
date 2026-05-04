package com.cybzacg.blogbackend.common.constant;

import java.time.Duration;

/**
 * 认证相关常量。<p>定义 Token 类型、Redis Key 前缀、邮箱验证码和登录失败锁定等常量。
 */
public final class AuthConstants {
    public static final String BEARER = "Bearer";
    public static final String TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String AUTHORITIES = "authorities";
    public static final String USER_ID = "userId";

    public static final String REDIS_AUTH_TOKEN_PREFIX = "auth:token";
    public static final String REDIS_USER = "user";
    public static final String REDIS_USERNAME = "username";
    public static final String REDIS_SESSIONS = "sessions";

    public static final String EMAIL_LOGIN_CODE_PREFIX = "auth:email-login-code";
    public static final Duration EMAIL_LOGIN_CODE_TTL = Duration.ofMinutes(5);
    public static final String EMAIL_LOGIN_CODE_RATE_PREFIX = "auth:email-login-code:rate";
    public static final Duration EMAIL_LOGIN_CODE_RATE_TTL = Duration.ofSeconds(60);
    public static final String EMAIL_LOGIN_SUBJECT = "登录验证码";
    public static final String LOGIN_FAIL_COUNT_PREFIX = "auth:login-fail:count";
    public static final String LOGIN_FAIL_LOCK_PREFIX = "auth:login-fail:lock";
    public static final String LOGIN_FAIL_SCOPE_USER = "user";
    public static final String LOGIN_FAIL_SCOPE_ACCOUNT = "account";

    public static final String MFA_EMAIL_CODE_PREFIX = "auth:mfa-email-code";
    public static final Duration MFA_EMAIL_CODE_TTL = Duration.ofMinutes(5);
    public static final String MFA_EMAIL_CODE_RATE_PREFIX = "auth:mfa-email-code:rate";
    public static final Duration MFA_EMAIL_CODE_RATE_TTL = Duration.ofSeconds(60);
    public static final String MFA_TICKET_PREFIX = "auth:mfa-ticket";
    public static final Duration MFA_TICKET_TTL = Duration.ofMinutes(30);
    public static final String MFA_EMAIL_SUBJECT = "高风险操作二次验证码";
    public static final String TAKEOVER_TOKEN_PREFIX = "auth:takeover-token";
    public static final Duration TAKEOVER_TOKEN_TTL = Duration.ofMinutes(30);
    public static final String TOKEN_BLACKLIST_PREFIX = "auth:token-blacklist";

    public static final String PASSWORD_RESET_CODE_PREFIX = "auth:password-reset:code";
    public static final Duration PASSWORD_RESET_CODE_TTL = Duration.ofMinutes(5);
    public static final String PASSWORD_RESET_CODE_RATE_PREFIX = "auth:password-reset:code:rate";
    public static final Duration PASSWORD_RESET_CODE_RATE_TTL = Duration.ofSeconds(60);
    public static final String PASSWORD_RESET_SUBJECT = "密码重置验证码";

    private AuthConstants() {
    }
}
