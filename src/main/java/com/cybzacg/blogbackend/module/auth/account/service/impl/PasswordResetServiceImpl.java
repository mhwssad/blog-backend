package com.cybzacg.blogbackend.module.auth.account.service.impl;

import com.cybzacg.blogbackend.common.constant.AuthConstants;
import com.cybzacg.blogbackend.common.email.EmailService;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.account.service.PasswordResetService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 找回密码服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final SysUserRepository sysUserRepository;
    private final RedisOperator redisOperator;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendResetCode(String email) {
        String rateLimitKey = RedisKeyUtils.build(AuthConstants.PASSWORD_RESET_CODE_RATE_PREFIX, email);
        if (!redisOperator.setIfAbsent(rateLimitKey, "1", AuthConstants.PASSWORD_RESET_CODE_RATE_TTL)) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.EMAIL_CAPTCHA_RATE_LIMITED);
        }

        SysUser user = sysUserRepository.findByEmail(email);
        if (user == null) {
            return;
        }

        String code = generateCode();
        emailService.sendTextEmail(email, AuthConstants.PASSWORD_RESET_SUBJECT,
                "您的密码重置验证码为：" + code + "，5分钟内有效。");

        String codeKey = RedisKeyUtils.build(AuthConstants.PASSWORD_RESET_CODE_PREFIX, email);
        redisOperator.set(codeKey, code, AuthConstants.PASSWORD_RESET_CODE_TTL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String code, String newPassword) {
        String codeKey = RedisKeyUtils.build(AuthConstants.PASSWORD_RESET_CODE_PREFIX, email);
        Object storedCode = redisOperator.get(codeKey);

        if (storedCode == null) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.EMAIL_CAPTCHA_EXPIRED);
        }

        if (!MessageDigest.isEqual(
                code.getBytes(StandardCharsets.UTF_8),
                storedCode.toString().getBytes(StandardCharsets.UTF_8))) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.EMAIL_CAPTCHA_INVALID);
        }

        redisOperator.delete(codeKey);

        PasswordUtils.validate(newPassword);

        SysUser user = sysUserRepository.findByEmail(email);
        if (user == null) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserRepository.updateById(user);

        log.info("用户通过找回密码重置密码: userId={}", user.getId());
    }

    private String generateCode() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }
}
