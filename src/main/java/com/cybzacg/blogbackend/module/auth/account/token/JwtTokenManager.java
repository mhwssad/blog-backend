package com.cybzacg.blogbackend.module.auth.account.token;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.config.property.SecurityProperties;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserPrincipal;
import com.cybzacg.blogbackend.module.auth.account.model.AuthenticationToken;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * JWT Token 管理器
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "security.session", name = "type", havingValue = "jwt")
public class JwtTokenManager implements TokenManager {
    private final SecurityProperties securityProperties;
    private final RedisOperator redisOperator;
    private SecretKey secretKey;

    @PostConstruct
    void init() {
        SecurityProperties.SessionConfig session = getSessionConfig();
        SecurityProperties.JwtConfig jwtConfig = session.getJwt();
        if (jwtConfig == null || !StrUtils.hasText(jwtConfig.getSecretKey())) {
            throw new IllegalStateException("JWT 密钥未配置");
        }

        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成一组访问令牌与刷新令牌，并把用户身份与权限写入 JWT 载荷。
     */
    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        String username = resolveUsername(authentication);
        if (!StrUtils.hasText(username)) {
            throw new IllegalArgumentException("认证信息不能为空");
        }

        Instant now = Instant.now();
        Long userId = resolveUserId(authentication);
        List<String> authorities = extractAuthorities(authentication.getAuthorities());

        String accessToken = createToken(username, userId, authorities, AuthConstants.TOKEN_TYPE_ACCESS,
                now, getSessionConfig().getAccessTokenTimeToLive());
        String refreshToken = createToken(username, userId, authorities, AuthConstants.TOKEN_TYPE_REFRESH,
                now, getSessionConfig().getRefreshTokenTimeToLive());

        return AuthenticationToken.builder()
                .tokenType(AuthConstants.BEARER)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(getSessionConfig().getAccessTokenTimeToLive())
                .build();
    }

    @Override
    public Authentication parseToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, AuthConstants.TOKEN_TYPE_ACCESS);
        return buildAuthentication(claims);
    }

    @Override
    public boolean validateToken(String token) {
        if (!validate(token, AuthConstants.TOKEN_TYPE_ACCESS)) {
            return false;
        }
        return !isTokenBlacklisted(token);
    }

    @Override
    public boolean validateRefreshToken(String refreshToken) {
        return validate(refreshToken, AuthConstants.TOKEN_TYPE_REFRESH);
    }

    @Override
    public AuthenticationToken refreshToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, AuthConstants.TOKEN_TYPE_REFRESH);

        Authentication authentication = buildAuthentication(claims);
        return generateToken(authentication);
    }

    @Override
    public void invalidateUserSessions(Long userId) {
        if (userId == null) {
            return;
        }
        String blacklistKey = RedisKeyUtils.build(AuthConstants.TOKEN_BLACKLIST_PREFIX, userId);
        long now = Instant.now().getEpochSecond();
        redisOperator.set(blacklistKey, String.valueOf(now),
                Duration.ofSeconds(getSessionConfig().getAccessTokenTimeToLive()));
    }

    /**
     * 检查 token 是否属于已被加入黑名单的用户，且 token 签发时间早于黑名单加入时间。
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = parseClaims(token);
            Long userId = parseUserId(claims);
            if (userId == null) {
                return false;
            }
            String blacklistKey = RedisKeyUtils.build(AuthConstants.TOKEN_BLACKLIST_PREFIX, userId);
            String blacklistedAt = redisOperator.get(blacklistKey, String.class);
            if (!StrUtils.hasText(blacklistedAt)) {
                return false;
            }
            long blacklistTime = Long.parseLong(blacklistedAt);
            Date issuedAt = claims.getIssuedAt();
            return issuedAt != null && issuedAt.getTime() / 1000 < blacklistTime;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean validate(String token, String expectedTokenType) {
        try {
            Claims claims = parseClaims(token);
            return expectedTokenType.equals(claims.get(AuthConstants.TOKEN_TYPE, String.class));
        } catch (BusinessException | JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 统一解析并校验 JWT 声明，失败时转换为业务异常。
     */
    private Claims parseClaims(String token) {
        ExceptionThrowerCore.throwBusinessIfBlank(token, ResultErrorCode.INVALID_TOKEN, "Token不能为空");

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(normalizeToken(token))
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.INVALID_TOKEN, "Token无效或已过期", ex);
            throw new IllegalStateException("unreachable");
        }
    }

    private String createToken(String subject, Long userId, List<String> authorities, String tokenType,
                               Instant issuedAt, Integer ttlSeconds) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(AuthConstants.TOKEN_TYPE, tokenType);
        claims.put(AuthConstants.AUTHORITIES, authorities);
        if (userId != null) {
            claims.put(AuthConstants.USER_ID, userId);
        }

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .claims(claims)
                .signWith(secretKey);

        if (ttlSeconds != null && ttlSeconds > 0) {
            builder.expiration(Date.from(issuedAt.plusSeconds(ttlSeconds)));
        }

        return builder.compact();
    }

    private Authentication buildAuthentication(Claims claims) {
        String username = claims.getSubject();
        Long userId = parseUserId(claims);
        List<SimpleGrantedAuthority> authorities = extractAuthorities(claims);
        AuthUserPrincipal principal = new AuthUserPrincipal(userId, username);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object authoritiesValue = claims.get(AuthConstants.AUTHORITIES);
        if (!(authoritiesValue instanceof Collection<?> collection)) {
            return List.of();
        }

        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(StrUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    /**
     * 将 Spring Security 权限集合压平为可写入 Token 的字符串列表。
     */
    private List<String> extractAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return List.of();
        }

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StrUtils::hasText)
                .distinct()
                .toList();
    }

    private void validateTokenType(Claims claims, String expectedTokenType) {
        String actualTokenType = claims.get(AuthConstants.TOKEN_TYPE, String.class);
        ExceptionThrowerCore.throwBusinessIf(!expectedTokenType.equals(actualTokenType),
                ResultErrorCode.INVALID_TOKEN, "Token类型不匹配");
    }

    private String normalizeToken(String token) {
        String value = StrUtils.trim(token);
        if (value.regionMatches(true, 0, AuthConstants.BEARER + " ", 0, AuthConstants.BEARER.length() + 1)) {
            return value.substring(AuthConstants.BEARER.length() + 1);
        }
        return value;
    }

    private SecurityProperties.SessionConfig getSessionConfig() {
        if (securityProperties.getSession() == null) {
            throw new IllegalStateException("安全会话配置缺失");
        }
        return securityProperties.getSession();
    }

    /**
     * 从不同认证主体中提取用户名，兼容用户详情对象与简化主体对象。
     */
    private String resolveUsername(Authentication authentication) {
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
        return authentication.getName();
    }

    /**
     * 从不同认证主体中提取用户 ID，供 Token 载荷回填。
     */
    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUserDetails userDetails) {
            return userDetails.getUserId();
        }
        if (principal instanceof AuthUserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }
        return null;
    }

    private Long parseUserId(Claims claims) {
        Object userId = claims.get(AuthConstants.USER_ID);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
