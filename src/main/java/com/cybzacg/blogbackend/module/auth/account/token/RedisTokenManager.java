package com.cybzacg.blogbackend.module.auth.account.token;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.config.property.SecurityProperties;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserPrincipal;
import com.cybzacg.blogbackend.module.auth.account.model.AuthenticationToken;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.ReflectionUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

/**
 * Redis Token 管理器
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "security.session", name = "type", havingValue = "redis-token")
public class RedisTokenManager implements TokenManager {
    private final RedisOperator redisOperator;
    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成不透明访问令牌与刷新令牌，并把会话状态落到 Redis。
     */
    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalArgumentException("认证信息不能为空");
        }

        TokenContext context = buildTokenContext(authentication);
        SecurityProperties.SessionConfig sessionConfig = getSessionConfig();

        if (!Boolean.TRUE.equals(getRedisTokenConfig().getAllowMultiLogin())) {
            invalidateSessionsByUsername(context.getUsername());
        }

        String accessToken = generateOpaqueToken();
        String refreshToken = generateOpaqueToken();

        AccessTokenState accessTokenState = new AccessTokenState(
                context.getUserId(),
                context.getUsername(),
                context.getAuthorities(),
                refreshToken
        );
        RefreshTokenState refreshTokenState = new RefreshTokenState(
                context.getUserId(),
                context.getUsername(),
                context.getAuthorities(),
                accessToken
        );

        storeAccessToken(accessToken, accessTokenState, sessionConfig.getAccessTokenTimeToLive());
        storeRefreshToken(refreshToken, refreshTokenState, sessionConfig.getRefreshTokenTimeToLive());
        bindSessionIndex(accessToken, context);

        return AuthenticationToken.builder()
                .tokenType(AuthConstants.BEARER)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(sessionConfig.getAccessTokenTimeToLive())
                .build();
    }

    @Override
    public Authentication parseToken(String token) {
        AccessTokenState tokenState = getAccessTokenState(token);
        return buildAuthentication(tokenState);
    }

    @Override
    public boolean validateToken(String token) {
        return findAccessTokenState(token) != null;
    }

    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return findRefreshTokenState(refreshToken) != null;
    }

    @Override
    public AuthenticationToken refreshToken(String token) {
        String normalizedRefreshToken = normalizeToken(token);
        RefreshTokenState refreshTokenState = findRefreshTokenState(normalizedRefreshToken);
        ExceptionThrowerCore.throwBusinessIfNull(refreshTokenState, ResultErrorCode.INVALID_TOKEN,
                "Refresh Token无效或已过期");

        invalidateAccessToken(refreshTokenState.getAccessToken());
        Authentication authentication = buildAuthentication(refreshTokenState);
        return generateToken(authentication);
    }

    /**
     * 同时兼容访问令牌和刷新令牌的失效处理，尽量清理完整会话链路。
     */
    @Override
    public void invalidateToken(String token) {
        String normalizedToken = normalizeToken(token);
        AccessTokenState accessTokenState = findAccessTokenState(normalizedToken);
        if (accessTokenState != null) {
            invalidateAccessToken(normalizedToken, accessTokenState);
            return;
        }

        RefreshTokenState refreshTokenState = findRefreshTokenState(normalizedToken);
        if (refreshTokenState != null) {
            invalidateAccessToken(refreshTokenState.getAccessToken());
            redisOperator.delete(refreshTokenKey(normalizedToken));
        }
    }

    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }
        invalidateSessions(userSessionKey(userId));
    }

    private void storeAccessToken(String accessToken, AccessTokenState tokenState, Integer ttlSeconds) {
        Duration ttl = toDuration(ttlSeconds);
        if (ttl == null) {
            redisOperator.set(accessTokenKey(accessToken), tokenState);
            return;
        }
        redisOperator.set(accessTokenKey(accessToken), tokenState, ttl);
    }

    private void storeRefreshToken(String refreshToken, RefreshTokenState tokenState, Integer ttlSeconds) {
        Duration ttl = toDuration(ttlSeconds);
        if (ttl == null) {
            redisOperator.set(refreshTokenKey(refreshToken), tokenState);
            return;
        }
        redisOperator.set(refreshTokenKey(refreshToken), tokenState, ttl);
    }

    private AccessTokenState getAccessTokenState(String token) {
        AccessTokenState tokenState = findAccessTokenState(token);
        ExceptionThrowerCore.throwBusinessIfNull(tokenState, ResultErrorCode.INVALID_TOKEN,
                "Token无效或已过期");
        return tokenState;
    }

    private AccessTokenState findAccessTokenState(String token) {
        return redisOperator.get(accessTokenKey(normalizeToken(token)), AccessTokenState.class);
    }

    private RefreshTokenState findRefreshTokenState(String token) {
        return redisOperator.get(refreshTokenKey(normalizeToken(token)), RefreshTokenState.class);
    }

    private void bindSessionIndex(String accessToken, TokenContext context) {
        redisOperator.setAdd(usernameSessionKey(context.getUsername()), accessToken);
        if (context.getUserId() != null) {
            redisOperator.setAdd(userSessionKey(context.getUserId()), accessToken);
        }
    }

    private void invalidateAccessToken(String accessToken) {
        AccessTokenState tokenState = findAccessTokenState(accessToken);
        if (tokenState != null) {
            invalidateAccessToken(accessToken, tokenState);
            return;
        }

        redisOperator.delete(accessTokenKey(accessToken));
    }

    private void invalidateAccessToken(String accessToken, AccessTokenState tokenState) {
        redisOperator.delete(accessTokenKey(accessToken));
        if (StringUtils.hasText(tokenState.getRefreshToken())) {
            redisOperator.delete(refreshTokenKey(tokenState.getRefreshToken()));
        }
        redisOperator.setRemove(usernameSessionKey(tokenState.getUsername()), accessToken);
        if (tokenState.getUserId() != null) {
            redisOperator.setRemove(userSessionKey(tokenState.getUserId()), accessToken);
        }
    }

    private void invalidateSessionsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        invalidateSessions(usernameSessionKey(username));
    }

    private void invalidateSessions(String sessionKey) {
        Set<Object> tokens = new LinkedHashSet<>(redisOperator.members(sessionKey));
        for (Object token : tokens) {
            if (token != null) {
                invalidateAccessToken(String.valueOf(token));
            }
        }
        redisOperator.delete(sessionKey);
    }

    private Authentication buildAuthentication(TokenState tokenState) {
        AuthUserPrincipal principal = new AuthUserPrincipal(tokenState.getUserId(), tokenState.getUsername());
        List<SimpleGrantedAuthority> authorities = tokenState.getAuthorities() == null
                ? List.of()
                : tokenState.getAuthorities().stream()
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    private TokenContext buildTokenContext(Authentication authentication) {
        return new TokenContext(
                resolveUserId(authentication),
                authentication.getName(),
                extractAuthorities(authentication.getAuthorities())
        );
    }

    private List<String> extractAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return List.of();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Long userId = tryExtractUserId(authentication.getPrincipal());
        if (userId != null) {
            return userId;
        }
        return tryExtractUserId(authentication.getDetails());
    }

    /**
     * 尝试从多种主体对象结构中提取用户 ID，兼容 details、principal 和反射回退。
     */
    private Long tryExtractUserId(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Number number) {
            return number.longValue();
        }
        if (source instanceof AuthUserDetails userDetails) {
            return userDetails.getUserId();
        }
        if (source instanceof AuthUserPrincipal principal) {
            return principal.getUserId();
        }

        Long userId = invokeLongGetter(source, "getUserId");
        if (userId != null) {
            return userId;
        }

        userId = invokeLongGetter(source, "getId");
        if (userId != null) {
            return userId;
        }

        userId = readLongField(source, "userId");
        if (userId != null) {
            return userId;
        }

        return readLongField(source, "id");
    }

    private Long invokeLongGetter(Object source, String methodName) {
        Object value = ReflectionUtils.invokeNoArgMethod(source, methodName).orElse(null);
        return value instanceof Number number ? number.longValue() : null;
    }

    private Long readLongField(Object source, String fieldName) {
        Object value = ReflectionUtils.readField(source, fieldName).orElse(null);
        return value instanceof Number number ? number.longValue() : null;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeToken(String token) {
        ExceptionThrowerCore.throwBusinessIfBlank(token, ResultErrorCode.INVALID_TOKEN, "Token不能为空");

        String value = StrUtils.trim(token);
        if (value.regionMatches(true, 0, AuthConstants.BEARER + " ", 0, AuthConstants.BEARER.length() + 1)) {
            return value.substring(AuthConstants.BEARER.length() + 1);
        }
        return value;
    }

    private Duration toDuration(Integer ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return null;
        }
        return Duration.ofSeconds(ttlSeconds);
    }

    private String accessTokenKey(String accessToken) {
        return RedisKeyUtils.build(AuthConstants.REDIS_AUTH_TOKEN_PREFIX, AuthConstants.TOKEN_TYPE_ACCESS, accessToken);
    }

    private String refreshTokenKey(String refreshToken) {
        return RedisKeyUtils.build(AuthConstants.REDIS_AUTH_TOKEN_PREFIX, AuthConstants.TOKEN_TYPE_REFRESH, refreshToken);
    }

    private String userSessionKey(Long userId) {
        return RedisKeyUtils.build(AuthConstants.REDIS_AUTH_TOKEN_PREFIX, AuthConstants.REDIS_USER, userId, AuthConstants.REDIS_SESSIONS);
    }

    private String usernameSessionKey(String username) {
        return RedisKeyUtils.build(AuthConstants.REDIS_AUTH_TOKEN_PREFIX, AuthConstants.REDIS_USERNAME, username, AuthConstants.REDIS_SESSIONS);
    }

    private SecurityProperties.SessionConfig getSessionConfig() {
        if (securityProperties.getSession() == null) {
            throw new IllegalStateException("安全会话配置缺失");
        }
        return securityProperties.getSession();
    }

    private SecurityProperties.RedisTokenConfig getRedisTokenConfig() {
        SecurityProperties.RedisTokenConfig redisTokenConfig = getSessionConfig().getRedisToken();
        return redisTokenConfig != null ? redisTokenConfig : new SecurityProperties.RedisTokenConfig();
    }

    private interface TokenState {
        Long getUserId();

        String getUsername();

        List<String> getAuthorities();
    }

    @Data
    @AllArgsConstructor
    private static class TokenContext {
        private Long userId;
        private String username;
        private List<String> authorities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AccessTokenState implements TokenState {
        private Long userId;
        private String username;
        private List<String> authorities;
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class RefreshTokenState implements TokenState {
        private Long userId;
        private String username;
        private List<String> authorities;
        private String accessToken;
    }
}
