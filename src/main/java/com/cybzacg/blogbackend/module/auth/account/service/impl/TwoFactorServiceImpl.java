package com.cybzacg.blogbackend.module.auth.account.service.impl;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.email.EmailService;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.service.TwoFactorService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * 二次验证（2FA/MFA）服务实现。
 *
 * <p>负责发送和校验2FA邮箱验证码，生成并验证操作票据。
 */
@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {
    private final SysUserRepository sysUserRepository;
    private final RedisOperator redisOperator;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendMfaCode(Long userId) {
        String email = sysUserRepository.findEmailById(userId);
        ExceptionThrowerCore.throwBusinessIfBlank(email, ResultErrorCode.SUPER_ADMIN_EMAIL_REQUIRED);

        String rateLimitKey = RedisKeyUtils.build(AuthConstants.MFA_EMAIL_CODE_RATE_PREFIX, userId);
        ExceptionThrowerCore.throwBusinessIfNot(
                redisOperator.setIfAbsent(rateLimitKey, "1", AuthConstants.MFA_EMAIL_CODE_RATE_TTL),
                ResultErrorCode.MFA_EMAIL_CODE_RATE_LIMITED);

        String code = generateCode();
        emailService.sendTextEmail(email, AuthConstants.MFA_EMAIL_SUBJECT, buildContent(code));

        redisOperator.set(mfaCodeKey(userId), code, AuthConstants.MFA_EMAIL_CODE_TTL);
    }

    @Override
    public String verifyMfaCode(Long userId, String code) {
        String storedCode = redisOperator.get(mfaCodeKey(userId), String.class);
        ExceptionThrowerCore.throwBusinessIfBlank(storedCode, ResultErrorCode.MFA_EMAIL_CODE_INVALID);
        ExceptionThrowerCore.throwBusinessIfNot(MessageDigest.isEqual(
                storedCode.getBytes(StandardCharsets.UTF_8),
                code.getBytes(StandardCharsets.UTF_8)), ResultErrorCode.MFA_EMAIL_CODE_INVALID);

        redisOperator.delete(mfaCodeKey(userId));

        String ticket = UUID.randomUUID().toString();
        redisOperator.set(mfaTicketKey(userId, ticket), "1", AuthConstants.MFA_TICKET_TTL);
        return ticket;
    }

    @Override
    public boolean validateTicket(String ticket, Long userId) {
        if (!StringUtils.hasText(ticket) || userId == null) {
            return false;
        }
        String ticketKey = mfaTicketKey(userId, ticket);
        if (!redisOperator.hasKey(ticketKey)) {
            return false;
        }
        // refresh TTL on successful validation
        redisOperator.expire(ticketKey, AuthConstants.MFA_TICKET_TTL);
        return true;
    }

    private String generateCode() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    private String buildContent(String code) {
        return "您正在进行高风险操作，验证码为：" + code + "，5分钟内有效。";
    }

    private String mfaCodeKey(Long userId) {
        return RedisKeyUtils.build(AuthConstants.MFA_EMAIL_CODE_PREFIX, userId);
    }

    private String mfaTicketKey(Long userId, String ticket) {
        return RedisKeyUtils.build(AuthConstants.MFA_TICKET_PREFIX, userId, ticket);
    }
}
