package com.cybzacg.blogbackend.core.filter;

import com.cybzacg.blogbackend.common.constant.ConfigConstants;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.core.util.HttpServletResponseUtils;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.utils.IPUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 基于客户端 IP 的全局请求限流过滤器。
 *
 * <p>默认按“每 IP 每秒最多 10 次请求”执行固定窗口限流，支持通过系统配置覆盖阈值。
 * Redis 异常时按可用性优先放行请求，仅记录告警日志，避免全站被限流组件反向阻断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpRateLimitFilter extends OncePerRequestFilter {
    private static final long RATE_LIMIT_WINDOW_SECONDS = 1L;

    private final RedisOperator redisOperator;
    private final SysConfigService sysConfigService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (!isRequestAllowed(resolveClientIp(request))) {
            HttpServletResponseUtils.writeJson(response, 429,
                    ResultErrorCode.REQUEST_RATE_LIMITED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 基于“IP + 秒级时间窗口”执行固定窗口限流，命中阈值时直接拒绝请求。
     */
    private boolean isRequestAllowed(String clientIp) {
        int limit = resolveRateLimitPerSecond();
        if (limit <= 0) {
            return true;
        }
        try {
            String rateKey = RedisKeyUtils.build(
                    RedisConstants.IP_RATE_LIMIT_KEY_PREFIX,
                    normalizeIpSegment(clientIp),
                    Instant.now().getEpochSecond());
            long requestCount = redisOperator.increment(rateKey);
            if (requestCount == 1L) {
                redisOperator.expire(rateKey, RATE_LIMIT_WINDOW_SECONDS + 1L, TimeUnit.SECONDS);
            }
            return requestCount <= limit;
        } catch (Exception ex) {
            log.warn("全局IP限流执行失败, ip={}", clientIp, ex);
            return true;
        }
    }

    /**
     * 从系统配置读取秒级阈值，未配置或格式非法时回退到默认值。
     */
    private int resolveRateLimitPerSecond() {
        String configValue = sysConfigService.getValueOrDefault(
                ConfigConstants.SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY,
                String.valueOf(ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND));
        String normalizedValue = StrUtils.trimToNull(configValue);
        if (normalizedValue == null) {
            return ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND;
        }
        try {
            return Integer.parseInt(normalizedValue);
        } catch (NumberFormatException ex) {
            log.warn("全局IP限流配置非法, configKey={}, value={}, fallback={}",
                    ConfigConstants.SECURITY_IP_RATE_LIMIT_PER_SECOND_KEY,
                    normalizedValue,
                    ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND);
            return ConfigConstants.DEFAULT_SECURITY_IP_RATE_LIMIT_PER_SECOND;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String clientIp = StrUtils.trimToNull(IPUtils.getIpAddr(request));
        return clientIp != null ? clientIp : "unknown";
    }

    private String normalizeIpSegment(String clientIp) {
        return clientIp.replace(':', '_');
    }
}

