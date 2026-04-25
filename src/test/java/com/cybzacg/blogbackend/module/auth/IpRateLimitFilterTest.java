package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.core.filter.IpRateLimitFilter;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.service.SysConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpRateLimitFilterTest {
    @Mock
    private RedisOperator redisOperator;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private FilterChain filterChain;

    @Test
    void shouldAllowRequestWhenWithinDefaultLimit() throws ServletException, IOException {
        TestableIpRateLimitFilter filter = new TestableIpRateLimitFilter(redisOperator, sysConfigService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY,
                String.valueOf(ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND))).thenReturn("10");
        when(redisOperator.increment(anyString())).thenReturn(1L);

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(redisOperator).expire(anyString(), eq(2L), any());
    }

    @Test
    void shouldRejectRequestWhenConfiguredLimitExceeded() throws ServletException, IOException {
        TestableIpRateLimitFilter filter = new TestableIpRateLimitFilter(redisOperator, sysConfigService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.11");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY,
                String.valueOf(ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND))).thenReturn("2");
        when(redisOperator.increment(anyString())).thenReturn(3L);

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertTrue(response.getContentAsString().contains(String.valueOf(ResultErrorCode.REQUEST_RATE_LIMITED.getCode())));
    }

    @Test
    void shouldFallbackToDefaultLimitWhenConfigInvalid() throws ServletException, IOException {
        TestableIpRateLimitFilter filter = new TestableIpRateLimitFilter(redisOperator, sysConfigService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.12");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY,
                String.valueOf(ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND))).thenReturn("invalid");
        when(redisOperator.increment(anyString())).thenReturn(11L);

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertTrue(response.getContentAsString().contains(String.valueOf(ResultErrorCode.REQUEST_RATE_LIMITED.getCode())));
    }

    @Test
    void shouldAllowRequestWhenLimitDisabled() throws ServletException, IOException {
        TestableIpRateLimitFilter filter = new TestableIpRateLimitFilter(redisOperator, sysConfigService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.13");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY,
                String.valueOf(ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND))).thenReturn("0");

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(redisOperator, never()).increment(anyString());
    }

    @Test
    void shouldPassThroughWhenRedisThrows() throws ServletException, IOException {
        TestableIpRateLimitFilter filter = new TestableIpRateLimitFilter(redisOperator, sysConfigService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.14");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(sysConfigService.getValueOrDefault(
                ConfigConstants.SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY,
                String.valueOf(ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND))).thenReturn("10");
        when(redisOperator.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipOptionsRequest() throws ServletException, IOException {
        TestableIpRateLimitFilter filter = new TestableIpRateLimitFilter(redisOperator, sysConfigService);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        request.setRemoteAddr("192.168.0.15");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(sysConfigService, never()).getValueOrDefault(anyString(), anyString());
        verify(redisOperator, never()).increment(anyString());
    }

    private static final class TestableIpRateLimitFilter extends IpRateLimitFilter {
        private TestableIpRateLimitFilter(RedisOperator redisOperator, SysConfigService sysConfigService) {
            super(redisOperator, sysConfigService);
        }

        private void invokeFilter(MockHttpServletRequest request,
                                  MockHttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
            super.doFilter(request, response, filterChain);
        }
    }
}
