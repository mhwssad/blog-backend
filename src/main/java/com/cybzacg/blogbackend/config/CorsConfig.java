package com.cybzacg.blogbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域资源共享（CORS）配置。<p>注册全局 CORS 过滤器，允许指定来源、请求头和方法的跨域请求，并置于 Spring Security 过滤器之前执行。</p>
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    /**
     * 注册 CORS 过滤器，优先级高于 Spring Security 过滤器。
     *
     * @return CORS 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<?> filterRegistrationBean() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //1.允许的来源（从配置文件读取，默认 *）
        corsConfiguration.setAllowedOriginPatterns(allowedOrigins);
        //2.允许任何请求头
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        //3.允许任何方法
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
        //4.允许凭证
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        CorsFilter corsFilter = new CorsFilter(source);

        FilterRegistrationBean<CorsFilter> filterRegistrationBean = new FilterRegistrationBean<>(corsFilter);
        filterRegistrationBean.setOrder(-101);  // 小于 SpringSecurity Filter的 Order(-100) 即可

        return filterRegistrationBean;
    }
}
