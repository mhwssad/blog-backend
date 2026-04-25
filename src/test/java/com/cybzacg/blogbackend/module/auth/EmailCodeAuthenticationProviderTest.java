package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.authentication.EmailCodeAuthenticationProvider;
import com.cybzacg.blogbackend.module.auth.authentication.EmailCodeAuthenticationToken;
import com.cybzacg.blogbackend.module.auth.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.service.AuthUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailCodeAuthenticationProviderTest {
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private AuthUserDetailsService authUserDetailsService;

    private EmailCodeAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new EmailCodeAuthenticationProvider(redisOperator, authUserDetailsService);
    }

    @Test
    void authenticateShouldRejectExpiredEmailCode() {
        EmailCodeAuthenticationToken token = EmailCodeAuthenticationToken.unauthenticated("demo@example.com", "9527");
        when(redisOperator.get(emailCodeKey("demo@example.com"), String.class)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> provider.authenticate(token));

        assertEquals(ResultErrorCode.EMAIL_CAPTCHA_EXPIRED.getCode(), exception.getCode());
        verify(authUserDetailsService, never()).loadAuthUserByUsername("demo@example.com");
    }

    @Test
    void authenticateShouldRejectDisabledUser() {
        EmailCodeAuthenticationToken token = EmailCodeAuthenticationToken.unauthenticated("demo@example.com", "9527");
        when(redisOperator.get(emailCodeKey("demo@example.com"), String.class)).thenReturn("9527");
        when(authUserDetailsService.loadAuthUserByUsername("demo@example.com")).thenReturn(userDetails(7L, "demo@example.com", 0));

        DisabledException exception = assertThrows(DisabledException.class, () -> provider.authenticate(token));

        assertEquals(ResultErrorCode.ACCOUNT_DISABLED.getMessage(), exception.getMessage());
        verify(redisOperator, never()).delete(emailCodeKey("demo@example.com"));
    }

    @Test
    void authenticateShouldDeleteCodeAfterSuccessfulLogin() {
        EmailCodeAuthenticationToken token = EmailCodeAuthenticationToken.unauthenticated("demo@example.com", "9527");
        when(redisOperator.get(emailCodeKey("demo@example.com"), String.class)).thenReturn("9527");
        when(authUserDetailsService.loadAuthUserByUsername("demo@example.com")).thenReturn(userDetails(7L, "demo@example.com", 1));

        Authentication result = provider.authenticate(token);

        assertTrue(result.isAuthenticated());
        assertEquals("demo@example.com", result.getName());
        verify(redisOperator).delete(emailCodeKey("demo@example.com"));
    }

    private AuthUserDetails userDetails(Long userId, String username, Integer status) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setPassword("encoded");
        user.setNickname("Demo");
        user.setStatus(status);
        return AuthUserDetails.of(
                user,
                List.of("user"),
                List.of("auth:login"),
                List.of(new SimpleGrantedAuthority("ROLE_user"), new SimpleGrantedAuthority("auth:login"))
        );
    }

    private String emailCodeKey(String email) {
        return RedisKeyUtils.build(AuthConstants.EMAIL_LOGIN_CODE_PREFIX, email);
    }
}
