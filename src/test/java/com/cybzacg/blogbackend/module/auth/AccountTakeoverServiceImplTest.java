package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.module.auth.account.model.AuthUserDetails;
import com.cybzacg.blogbackend.module.auth.account.model.admin.AccountTakeoverResponse;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.AuthUserDetailsService;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.account.service.TwoFactorService;
import com.cybzacg.blogbackend.module.auth.account.service.impl.AccountTakeoverServiceImpl;
import com.cybzacg.blogbackend.module.auth.audit.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountTakeoverServiceImplTest {
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private SysAuditLogService sysAuditLogService;
    @Mock
    private SuperAdminVerifier superAdminVerifier;
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private AuthUserDetailsService authUserDetailsService;

    private AccountTakeoverServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountTakeoverServiceImpl(
                sysUserRepository, twoFactorService, sysAuditLogService,
                superAdminVerifier, redisOperator, authUserDetailsService
        );
    }

    @Test
    void takeoverShouldCreateOneTimeTokenAndAuditLog() {
        SysUser targetUser = new SysUser();
        targetUser.setId(9L);
        targetUser.setUsername("target");
        when(twoFactorService.validateTicket("ticket-1", 1L)).thenReturn(true);
        when(sysUserRepository.getById(9L)).thenReturn(targetUser);

        AccountTakeoverResponse response = service.takeover(1L, 9L, "ticket-1", "127.0.0.1", "ua");

        assertEquals(9L, response.getTargetUserId());
        assertEquals("target", response.getTargetUsername());
        verify(superAdminVerifier).requireSuperAdmin(1L);
        verify(redisOperator).set(eq(RedisKeyUtils.build(AuthConstants.TAKEOVER_TOKEN_PREFIX, response.getTakeoverToken())),
                eq("9"), eq(AuthConstants.TAKEOVER_TOKEN_TTL));
        ArgumentCaptor<SysAuditLogCreateRequest> captor = ArgumentCaptor.forClass(SysAuditLogCreateRequest.class);
        verify(sysAuditLogService).record(captor.capture());
        assertEquals(SysAuditOperationType.TAKEOVER_ACCOUNT.getCode(), captor.getValue().getOperationType());
    }

    @Test
    void resolveTakeoverShouldLoadUserAndConsumeToken() {
        String tokenKey = RedisKeyUtils.build(AuthConstants.TAKEOVER_TOKEN_PREFIX, "token-1");
        SysUser targetUser = new SysUser();
        targetUser.setId(9L);
        targetUser.setUsername("target");
        AuthUserDetails details = AuthUserDetails.builder()
                .userId(9L)
                .username("target")
                .authorities(java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_user")))
                .build();

        when(redisOperator.get(tokenKey, String.class)).thenReturn("9");
        when(sysUserRepository.getById(9L)).thenReturn(targetUser);
        when(authUserDetailsService.loadAuthUserByUsername("target")).thenReturn(details);

        var authentication = service.resolveTakeover("token-1");

        assertEquals("target", authentication.getName());
        verify(redisOperator).delete(tokenKey);
    }
}
