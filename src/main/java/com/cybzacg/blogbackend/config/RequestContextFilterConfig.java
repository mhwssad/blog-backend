package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.core.filter.RequestContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 请求上下文过滤器注册配置。
 */
@Configuration
public class RequestContextFilterConfig {

    @Bean
    public RequestContextFilter ttlRequestContextFilter() {
        return new RequestContextFilter();
    }

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration(RequestContextFilter ttlRequestContextFilter) {
        FilterRegistrationBean<RequestContextFilter> registrationBean = new FilterRegistrationBean<>(ttlRequestContextFilter);
        registrationBean.setName("ttlRequestContextFilter");
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(-102);
        return registrationBean;
    }
}
