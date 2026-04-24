package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.core.filter.RequestContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 请求上下文过滤器注册配置。<p>注册 RequestContextFilter 并设定优先级为 -102，确保在 CORS 和 Security 过滤器之前执行，用于透传请求级别的上下文信息。</p>
 */
@Configuration
public class RequestContextFilterConfig {

    /**
     * 创建请求上下文过滤器实例。
     *
     * @return RequestContextFilter 实例
     */
    @Bean
    public RequestContextFilter ttlRequestContextFilter() {
        return new RequestContextFilter();
    }

    /**
     * 注册请求上下文过滤器，优先级高于 CORS 和 Security 过滤器。
     *
     * @param ttlRequestContextFilter 请求上下文过滤器实例
     * @return 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration(RequestContextFilter ttlRequestContextFilter) {
        FilterRegistrationBean<RequestContextFilter> registrationBean = new FilterRegistrationBean<>(ttlRequestContextFilter);
        registrationBean.setName("ttlRequestContextFilter");
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(-102);
        return registrationBean;
    }
}
