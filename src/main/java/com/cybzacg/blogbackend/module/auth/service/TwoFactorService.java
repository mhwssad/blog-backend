package com.cybzacg.blogbackend.module.auth.service;

/**
 * 二次验证（2FA/MFA）服务接口。
 */
public interface TwoFactorService {
    /**
     * 发送2FA邮箱验证码。
     */
    void sendMfaCode(Long userId);

    /**
     * 校验2FA验证码并返回操作票据。
     */
    String verifyMfaCode(Long userId, String code);

    /**
     * 校验操作票据是否有效。
     */
    boolean validateTicket(String ticket, Long userId);
}
