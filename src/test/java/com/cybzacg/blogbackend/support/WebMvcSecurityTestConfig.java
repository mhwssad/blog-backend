package com.cybzacg.blogbackend.support;

import com.cybzacg.blogbackend.core.filter.TokenAuthenticationFilter;
import com.cybzacg.blogbackend.module.auth.account.token.TokenManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class WebMvcSecurityTestConfig {
    @Bean
    TokenAuthenticationFilter tokenAuthenticationFilter(TokenManager tokenManager) {
        return new TokenAuthenticationFilter(tokenManager);
    }
}
