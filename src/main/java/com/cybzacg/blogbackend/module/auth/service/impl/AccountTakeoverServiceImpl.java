package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.enums.SysAuditOperationType;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.model.admin.AccountTakeoverResponse;
import com.cybzacg.blogbackend.module.auth.model.common.SysAuditLogCreateRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.service.AccountTakeoverService;
import com.cybzacg.blogbackend.module.auth.service.AuthUserDetailsService;
import com.cybzacg.blogbackend.module.auth.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.service.SysAuditLogService;
import com.cybzacg.blogbackend.module.auth.service.TwoFactorService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountTakeoverServiceImpl implements AccountTakeoverService {
    private final SysUserRepository sysUserRepository;
    private final TwoFactorService twoFactorService;
    private final SysAuditLogService sysAuditLogService;
    private final SuperAdminVerifier superAdminVerifier;
    private final RedisOperator redisOperator;
    private final AuthUserDetailsService authUserDetailsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountTakeoverResponse takeover(Long operatorId, Long targetUserId, String mfaTicket, String ip, String ua) {
        twoFactorService.validateTicket(mfaTicket, operatorId);
        superAdminVerifier.requireSuperAdmin(operatorId);
        ExceptionThrowerCore.throwBusinessIf(operatorId.equals(targetUserId), ResultErrorCode.CANNOT_MODIFY_SELF);

        SysUser targetUser = sysUserRepository.getById(targetUserId);
        ExceptionThrowerCore.throwBusinessIfNull(targetUser, ResultErrorCode.USER_NOT_FOUND);

        String takeoverToken = UUID.randomUUID().toString();
        String tokenKey = RedisKeyUtils.build(AuthConstants.TAKEOVER_TOKEN_PREFIX, takeoverToken);
        // Store targetUserId in Redis so we can resolve it later
        redisOperator.set(tokenKey, targetUserId.toString(), AuthConstants.TAKEOVER_TOKEN_TTL);

        // Record audit log
        SysAuditLogCreateRequest auditRequest = new SysAuditLogCreateRequest();
        auditRequest.setOperatorUserId(operatorId);
        auditRequest.setTargetUserId(targetUserId);
        auditRequest.setOperationType(SysAuditOperationType.TAKEOVER_ACCOUNT.getCode());
        auditRequest.setMfaPassed(1);
        auditRequest.setRequestIp(ip);
        auditRequest.setUserAgent(ua);
        sysAuditLogService.record(auditRequest);

        long expiresIn = AuthConstants.TAKEOVER_TOKEN_TTL.getSeconds();
        return new AccountTakeoverResponse(takeoverToken, targetUserId, targetUser.getUsername(), expiresIn);
    }

    @Override
    public Authentication resolveTakeover(String takeoverToken) {
        String tokenKey = RedisKeyUtils.build(AuthConstants.TAKEOVER_TOKEN_PREFIX, takeoverToken);
        String targetUserIdStr = redisOperator.get(tokenKey, String.class);
        ExceptionThrowerCore.throwBusinessIfBlank(targetUserIdStr, ResultErrorCode.INVALID_TOKEN);

        Long targetUserId = Long.valueOf(targetUserIdStr);
        SysUser targetUser = sysUserRepository.getById(targetUserId);
        ExceptionThrowerCore.throwBusinessIfNull(targetUser, ResultErrorCode.USER_NOT_FOUND);

        // Load full user details with authorities
        var userDetails = authUserDetailsService.loadAuthUserByUsername(targetUser.getUsername());
        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
    }
}
