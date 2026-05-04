package com.cybzacg.blogbackend.module.auth.account.service;

/**
 * 找回密码服务接口。
 */
public interface PasswordResetService {

    /**
     * 发送找回密码验证码。邮箱不存在时静默忽略（不报错，防枚举）。
     */
    void sendResetCode(String email);

    /**
     * 验证码校验通过后重置密码。
     */
    void resetPassword(String email, String code, String newPassword);
}
