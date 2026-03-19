package com.cybzacg.blogbackend.module.auth.token;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.config.property.SecurityProperties;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.model.AuthenticationToken;
import com.cybzacg.blogbackend.module.auth.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.model.AuthUserPrincipal;
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
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * JWT Token 管理器
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "security.session", name = "type", havingValue = "jwt")
public class JwtTokenManager implements TokenManager {
    private final SecurityProperties securityProperties;
    private SecretKey secretKey;

    @PostConstruct
    void init() {
        SecurityProperties.SessionConfig session = getSessionConfig();
        SecurityProperties.JwtConfig jwtConfig = session.getJwt();
        if (jwtConfig == null || !StringUtils.hasText(jwtConfig.getSecretKey())) {
            throw new IllegalStateException("JWT 密钥未配置");
        }

        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public AuthenticationToken generateToken(Authentication authentication) {
        String username = resolveUsername(authentication);
        if (!StringUtils.hasText(username)) {
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
        return validate(token, AuthConstants.TOKEN_TYPE_ACCESS);
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

    private boolean validate(String token, String expectedTokenType) {
        try {
            Claims claims = parseClaims(token);
            return expectedTokenType.equals(claims.get(AuthConstants.TOKEN_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("Token不能为空");
        }

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(normalizeToken(token))
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Token无效或已过期", ex);
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

        if (ttlSeconds != null && ttlSeconds >= 0) {
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
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();
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

    private void validateTokenType(Claims claims, String expectedTokenType) {
        String actualTokenType = claims.get(AuthConstants.TOKEN_TYPE, String.class);
        if (!expectedTokenType.equals(actualTokenType)) {
            throw new BusinessException("Token类型不匹配");
        }
    }

    private String normalizeToken(String token) {
        String value = token.trim();
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
