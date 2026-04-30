package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.config.property.SecurityProperties;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.model.AuthUserPrincipal;
import com.cybzacg.blogbackend.module.auth.model.AuthenticationToken;
import com.cybzacg.blogbackend.module.auth.token.JwtTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtTokenManagerTest {
    @Mock
    private RedisOperator redisOperator;

    private JwtTokenManager jwtTokenManager;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        SecurityProperties.SessionConfig session = new SecurityProperties.SessionConfig();
        session.setType("jwt");
        session.setAccessTokenTimeToLive(3600);
        session.setRefreshTokenTimeToLive(604800);
        SecurityProperties.JwtConfig jwt = new SecurityProperties.JwtConfig();
        jwt.setSecretKey("SecretKey012345678901234567890123456789");
        session.setJwt(jwt);
        properties.setSession(session);

        jwtTokenManager = new JwtTokenManager(properties, redisOperator);
        ReflectionTestUtils.invokeMethod(jwtTokenManager, "init");
    }

    private Authentication createAuthentication(Long userId, String username, String... authorities) {
        AuthUserPrincipal principal = new AuthUserPrincipal(userId, username);
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, grantedAuthorities);
    }

    @Test
    void shouldGenerateTokenWithCorrectStructure() {
        Authentication auth = createAuthentication(1L, "admin", "ROLE_ADMIN");
        AuthenticationToken token = jwtTokenManager.generateToken(auth);

        assertEquals(AuthConstants.BEARER, token.getTokenType());
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertEquals(3600, token.getExpiresIn());
    }

    @Test
    void shouldValidateAccessTokenSuccessfully() {
        Authentication auth = createAuthentication(1L, "admin", "ROLE_ADMIN");
        AuthenticationToken token = jwtTokenManager.generateToken(auth);

        assertTrue(jwtTokenManager.validateToken(token.getAccessToken()));
    }

    @Test
    void shouldRejectAccessTokenAsRefreshToken() {
        Authentication auth = createAuthentication(1L, "admin", "ROLE_ADMIN");
        AuthenticationToken token = jwtTokenManager.generateToken(auth);

        assertFalse(jwtTokenManager.validateRefreshToken(token.getAccessToken()));
    }

    @Test
    void shouldRejectRefreshTokenAsAccessToken() {
        Authentication auth = createAuthentication(1L, "admin", "ROLE_ADMIN");
        AuthenticationToken token = jwtTokenManager.generateToken(auth);

        assertFalse(jwtTokenManager.validateToken(token.getRefreshToken()));
    }

    @Test
    void shouldParseAccessTokenToAuthentication() {
        Authentication auth = createAuthentication(42L, "admin", "ROLE_ADMIN", "user:query");
        AuthenticationToken token = jwtTokenManager.generateToken(auth);

        Authentication parsed = jwtTokenManager.parseToken(token.getAccessToken());

        assertEquals("admin", parsed.getName());
        assertNotNull(parsed.getPrincipal());
        assertTrue(parsed.getPrincipal() instanceof AuthUserPrincipal);
        AuthUserPrincipal principal = (AuthUserPrincipal) parsed.getPrincipal();
        assertEquals(42L, principal.getUserId());
        assertEquals("admin", principal.getUsername());
        assertTrue(parsed.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(parsed.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("user:query")));
    }

    @Test
    void shouldRejectInvalidTokenFormat() {
        assertFalse(jwtTokenManager.validateToken("not-a-jwt-token"));
    }

    @Test
    void shouldRefreshTokenFromValidRefreshToken() {
        Authentication auth = createAuthentication(1L, "admin", "ROLE_ADMIN");
        AuthenticationToken originalToken = jwtTokenManager.generateToken(auth);

        AuthenticationToken refreshedToken = jwtTokenManager.refreshToken(originalToken.getRefreshToken());

        assertNotNull(refreshedToken.getAccessToken());
        assertNotNull(refreshedToken.getRefreshToken());
        assertTrue(jwtTokenManager.validateToken(refreshedToken.getAccessToken()));
    }

    @Test
    void shouldRejectTokenWithWrongSignature() {
        SecurityProperties otherProps = new SecurityProperties();
        SecurityProperties.SessionConfig otherSession = new SecurityProperties.SessionConfig();
        otherSession.setType("jwt");
        otherSession.setAccessTokenTimeToLive(3600);
        otherSession.setRefreshTokenTimeToLive(604800);
        SecurityProperties.JwtConfig otherJwt = new SecurityProperties.JwtConfig();
        otherJwt.setSecretKey("DifferentSecretKey01234567890123456789012");
        otherSession.setJwt(otherJwt);
        otherProps.setSession(otherSession);

        JwtTokenManager otherManager = new JwtTokenManager(otherProps, mock(RedisOperator.class));
        ReflectionTestUtils.invokeMethod(otherManager, "init");

        Authentication auth = createAuthentication(1L, "admin", "ROLE_ADMIN");
        AuthenticationToken token = jwtTokenManager.generateToken(auth);

        assertFalse(otherManager.validateToken(token.getAccessToken()));
    }

    @Test
    void shouldHandleTokenWithBearerPrefix() {
        Authentication auth = createAuthentication(1L, "admin", "ROLE_ADMIN");
        AuthenticationToken token = jwtTokenManager.generateToken(auth);

        Authentication parsed = jwtTokenManager.parseToken("Bearer " + token.getAccessToken());

        assertEquals("admin", parsed.getName());
    }

    @Test
    void parseTokenShouldThrowForInvalidToken() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> jwtTokenManager.parseToken("invalid-token-string"));

        assertNotNull(exception.getMessage());
    }
}



