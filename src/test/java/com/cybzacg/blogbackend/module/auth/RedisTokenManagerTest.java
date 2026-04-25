package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.config.property.SecurityProperties;
import com.cybzacg.blogbackend.module.auth.model.AuthUserPrincipal;
import com.cybzacg.blogbackend.module.auth.model.AuthenticationToken;
import com.cybzacg.blogbackend.module.auth.token.RedisTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenManagerTest {
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, Set<Object>> sets = new LinkedHashMap<>();
    @Mock
    private RedisOperator redisOperator;

    @BeforeEach
    void setUp() {
        values.clear();
        sets.clear();

        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(redisOperator).set(any(String.class), any(), any(Duration.class));
        doAnswer(invocation -> {
            values.remove(invocation.getArgument(0));
            sets.remove(invocation.getArgument(0));
            return true;
        }).when(redisOperator).delete(any(String.class));
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object[] members = invocation.getArguments().length > 1
                    ? java.util.Arrays.copyOfRange(invocation.getArguments(), 1, invocation.getArguments().length)
                    : new Object[0];
            sets.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).addAll(List.of(members));
            return (long) members.length;
        }).when(redisOperator).setAdd(any(String.class), any());
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Set<Object> members = sets.computeIfAbsent(key, ignored -> new LinkedHashSet<>());
            Object[] toRemove = invocation.getArguments().length > 1
                    ? java.util.Arrays.copyOfRange(invocation.getArguments(), 1, invocation.getArguments().length)
                    : new Object[0];
            long removed = 0L;
            for (Object value : toRemove) {
                if (members.remove(value)) {
                    removed++;
                }
            }
            return removed;
        }).when(redisOperator).setRemove(any(String.class), any());
        lenient().when(redisOperator.members(any(String.class))).thenAnswer(invocation ->
                new LinkedHashSet<>(sets.getOrDefault(invocation.getArgument(0), Set.of())));
        when(redisOperator.get(any(String.class), any(Class.class))).thenAnswer(invocation -> {
            Object value = values.get(invocation.getArgument(0));
            Class<?> clazz = invocation.getArgument(1);
            return clazz.isInstance(value) ? value : null;
        });
    }

    @Test
    void refreshTokenShouldInvalidateOldAccessAndRefreshTokens() {
        RedisTokenManager tokenManager = new RedisTokenManager(redisOperator, buildProperties(true));
        AuthenticationToken issued = tokenManager.generateToken(authentication(7L, "demo"));

        AuthenticationToken refreshed = tokenManager.refreshToken(issued.getRefreshToken());

        assertFalse(tokenManager.validateToken(issued.getAccessToken()));
        assertFalse(tokenManager.validateRefreshToken(issued.getRefreshToken()));
        assertTrue(tokenManager.validateToken(refreshed.getAccessToken()));
        assertTrue(tokenManager.validateRefreshToken(refreshed.getRefreshToken()));
    }

    @Test
    void generateTokenShouldInvalidatePreviousSessionsWhenMultiLoginDisabled() {
        RedisTokenManager tokenManager = new RedisTokenManager(redisOperator, buildProperties(false));

        AuthenticationToken first = tokenManager.generateToken(authentication(7L, "demo"));
        AuthenticationToken second = tokenManager.generateToken(authentication(7L, "demo"));

        assertFalse(tokenManager.validateToken(first.getAccessToken()));
        assertFalse(tokenManager.validateRefreshToken(first.getRefreshToken()));
        assertTrue(tokenManager.validateToken(second.getAccessToken()));
        assertTrue(tokenManager.validateRefreshToken(second.getRefreshToken()));
    }

    private SecurityProperties buildProperties(boolean allowMultiLogin) {
        SecurityProperties properties = new SecurityProperties();
        SecurityProperties.SessionConfig session = new SecurityProperties.SessionConfig();
        session.setType("redis-token");
        session.setAccessTokenTimeToLive(3600);
        session.setRefreshTokenTimeToLive(604800);
        SecurityProperties.RedisTokenConfig redisToken = new SecurityProperties.RedisTokenConfig();
        redisToken.setAllowMultiLogin(allowMultiLogin);
        session.setRedisToken(redisToken);
        properties.setSession(session);
        return properties;
    }

    private Authentication authentication(Long userId, String username) {
        AuthUserPrincipal principal = new AuthUserPrincipal(userId, username);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_user"), new SimpleGrantedAuthority("auth:login"))
        );
    }
}
