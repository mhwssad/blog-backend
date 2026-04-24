package com.cybzacg.blogbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置。<p>提供基于 BCrypt 算法的密码编码器 Bean，供安全模块统一使用。</p>
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 创建 BCrypt 密码编码器实例。
     *
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
