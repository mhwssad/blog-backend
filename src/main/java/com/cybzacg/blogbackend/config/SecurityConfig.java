package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.config.property.SecurityProperties;
import com.cybzacg.blogbackend.core.filter.IpRateLimitFilter;
import com.cybzacg.blogbackend.core.filter.TokenAuthenticationFilter;
import com.cybzacg.blogbackend.module.auth.authentication.EmailCodeAuthenticationProvider;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.service.AuthUserDetailsService;
import com.cybzacg.blogbackend.utils.HttpServletResponseUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置。<p>配置无状态会话、JWT/邮箱验证码认证提供器、URL 权限规则，以及 IP 限流和 Token 认证过滤器链。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final SecurityProperties securityProperties;
    private final AuthUserDetailsService authUserDetailsService;
    private final EmailCodeAuthenticationProvider emailCodeAuthenticationProvider;
    private final PasswordEncoder passwordEncoder;
    private final IpRateLimitFilter ipRateLimitFilter;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    /**
     * 构建安全过滤器链，配置认证、授权、会话管理及异常处理。
     *
     * @param http HttpSecurity 构建器
     * @return SecurityFilterChain 实例
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .authenticationProvider(emailCodeAuthenticationProvider)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ResultErrorCode.LOGIN_REQUIRED))
                        .accessDeniedHandler((request, response, exception) ->
                                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ResultErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(securityProperties.getUnsecuredUrls()).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(ipRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tokenAuthenticationFilter, IpRateLimitFilter.class);
        return http.build();
    }

    /**
     * 配置安全框架完全忽略的 URL 模式，绕过所有安全过滤器。
     *
     * @return WebSecurityCustomizer 实例
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(securityProperties.getIgnoreUrls());
    }

    /**
     * 创建基于数据库的 DAO 认证提供器，绑定用户详情服务和密码编码器。
     *
     * @return DaoAuthenticationProvider 实例
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(authUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 暴露 AuthenticationManager Bean，供手动认证场景使用。
     *
     * @param authenticationConfiguration 认证配置
     * @return AuthenticationManager 实例
     * @throws Exception 获取失败时抛出
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    private void writeErrorResponse(jakarta.servlet.http.HttpServletResponse response,
                                    int httpStatus,
                                    ResultErrorCode errorCode) throws java.io.IOException {
        HttpServletResponseUtils.writeJson(response, httpStatus, errorCode);
    }
}
