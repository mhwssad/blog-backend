package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.email.EmailService;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.impl.TwoFactorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceImplTest {
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private EmailService emailService;

    private TwoFactorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TwoFactorServiceImpl(sysUserRepository, redisOperator, emailService);
    }

    @Test
    void sendMfaCodeShouldSendEmailAndCacheCode() {
        when(sysUserRepository.findEmailById(7L)).thenReturn("admin@example.com");
        when(redisOperator.setIfAbsent(
                RedisKeyUtils.build(AuthConstants.MFA_EMAIL_CODE_RATE_PREFIX, 7L),
                "1",
                AuthConstants.MFA_EMAIL_CODE_RATE_TTL)).thenReturn(true);

        service.sendMfaCode(7L);

        verify(emailService).sendTextEmail(eq("admin@example.com"), eq(AuthConstants.MFA_EMAIL_SUBJECT), anyString());
        verify(redisOperator).set(eq(RedisKeyUtils.build(AuthConstants.MFA_EMAIL_CODE_PREFIX, 7L)),
                anyString(), eq(AuthConstants.MFA_EMAIL_CODE_TTL));
    }

    @Test
    void verifyMfaCodeShouldConsumeValidCode() {
        when(redisOperator.get(RedisKeyUtils.build(AuthConstants.MFA_EMAIL_CODE_PREFIX, 7L), String.class))
                .thenReturn("123456");

        String ticket = service.verifyMfaCode(7L, "123456");

        assertNotNull(ticket);
        verify(redisOperator).delete(RedisKeyUtils.build(AuthConstants.MFA_EMAIL_CODE_PREFIX, 7L));
        verify(redisOperator).set(eq(RedisKeyUtils.build(AuthConstants.MFA_TICKET_PREFIX, 7L, ticket)),
                eq("1"), eq(AuthConstants.MFA_TICKET_TTL));
    }

    @Test
    void validateTicketShouldRefreshExistingTicket() {
        String ticketKey = RedisKeyUtils.build(AuthConstants.MFA_TICKET_PREFIX, 7L, "ticket-1");
        when(redisOperator.hasKey(ticketKey)).thenReturn(true);

        assertTrue(service.validateTicket("ticket-1", 7L));
        verify(redisOperator).expire(ticketKey, AuthConstants.MFA_TICKET_TTL);
    }

    @Test
    void validateTicketShouldRejectMissingOrBlankTicket() {
        assertFalse(service.validateTicket(" ", 7L));
        assertFalse(service.validateTicket("ticket", null));
    }
}
