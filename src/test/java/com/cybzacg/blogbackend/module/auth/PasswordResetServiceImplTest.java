package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.account.service.impl.PasswordResetServiceImpl;
import com.cybzacg.blogbackend.common.email.EmailService;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(sysUserRepository, redisOperator, emailService, passwordEncoder);
    }

    @Test
    void sendResetCodeShouldSendEmailWhenUserExists() {
        when(redisOperator.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(sysUserRepository.findByEmail("test@example.com")).thenReturn(user());

        service.sendResetCode("test@example.com");

        verify(emailService).sendTextEmail(eq("test@example.com"), anyString(), anyString());
        verify(redisOperator).set(anyString(), anyString(), any());
    }

    @Test
    void sendResetCodeShouldSilentlyReturnWhenEmailNotRegistered() {
        when(redisOperator.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(sysUserRepository.findByEmail("none@example.com")).thenReturn(null);

        service.sendResetCode("none@example.com");

        verify(emailService, never()).sendTextEmail(anyString(), anyString(), anyString());
        verify(redisOperator, never()).set(anyString(), anyString(), any());
    }

    @Test
    void sendResetCodeShouldRejectWhenRateLimited() {
        when(redisOperator.setIfAbsent(anyString(), eq("1"), any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendResetCode("test@example.com"));
        assertEquals(ResultErrorCode.EMAIL_CAPTCHA_RATE_LIMITED.getCode(), ex.getCode());
    }

    @Test
    void resetPasswordShouldSucceedWithValidCode() {
        when(redisOperator.get(anyString())).thenReturn("123456");
        when(sysUserRepository.findByEmail("test@example.com")).thenReturn(user());
        when(passwordEncoder.encode("NewPass123")).thenReturn("encoded");

        service.resetPassword("test@example.com", "123456", "NewPass123");

        verify(redisOperator).delete(anyString());
        verify(sysUserRepository).updateById(any(SysUser.class));
    }

    @Test
    void resetPasswordShouldRejectExpiredCode() {
        when(redisOperator.get(anyString())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resetPassword("test@example.com", "123456", "NewPass123"));
        assertEquals(ResultErrorCode.EMAIL_CAPTCHA_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    void resetPasswordShouldRejectInvalidCode() {
        when(redisOperator.get(anyString())).thenReturn("654321");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resetPassword("test@example.com", "123456", "NewPass123"));
        assertEquals(ResultErrorCode.EMAIL_CAPTCHA_INVALID.getCode(), ex.getCode());
    }

    private SysUser user() {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setEmail("test@example.com");
        u.setPassword("oldEncoded");
        return u;
    }
}
