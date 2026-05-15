package com.cybzacg.blogbackend.core.filter;

import com.cybzacg.blogbackend.common.constant.HttpHeaderConstants;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 请求上下文过滤器。
 *
 * <p>在请求入口统一采集 traceId、客户端 IP、方法、路径和 User-Agent 等信息，
 * 写入可跨异步线程透传的上下文容器，供后续业务、日志和异常处理直接读取。
 */
public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        RequestContextUtils.bindRequest(request, traceId);
        response.setHeader(HttpHeaderConstants.X_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextUtils.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = StrUtils.trimToNull(request.getHeader(HttpHeaderConstants.X_TRACE_ID));
        if (traceId != null) {
            return traceId;
        }
        Object requestAttribute = request.getAttribute(RequestContextUtils.TRACE_ID_ATTRIBUTE);
        if (requestAttribute instanceof String attributeTraceId && StrUtils.hasText(attributeTraceId)) {
            return attributeTraceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
