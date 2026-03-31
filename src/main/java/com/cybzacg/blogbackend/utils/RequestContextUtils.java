package com.cybzacg.blogbackend.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Optional;

/**
 * HTTP 请求上下文工具类。
 *
 * <p>基于 {@link TransmittableContextUtils} 统一暴露请求链路中的通用字段，避免业务代码在
 * Controller / Service / 异步任务中重复解析 `HttpServletRequest`。
 */
public final class RequestContextUtils {
    public static final String TRACE_ID_ATTRIBUTE = "traceId";

    private static final String TRACE_ID_KEY = "request.traceId";
    private static final String CLIENT_IP_KEY = "request.clientIp";
    private static final String REQUEST_METHOD_KEY = "request.method";
    private static final String REQUEST_URI_KEY = "request.uri";
    private static final String QUERY_STRING_KEY = "request.queryString";
    private static final String USER_AGENT_KEY = "request.userAgent";

    private RequestContextUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 根据当前 HTTP 请求初始化统一上下文，并同步写入兼容用 request attribute。
     */
    public static void bindRequest(HttpServletRequest request, String traceId) {
        if (request == null) {
            return;
        }
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        TransmittableContextUtils.replace(Map.of(
                TRACE_ID_KEY, traceId,
                CLIENT_IP_KEY, defaultString(IPUtils.getIpAddr(request)),
                REQUEST_METHOD_KEY, defaultString(request.getMethod()),
                REQUEST_URI_KEY, defaultString(request.getRequestURI()),
                QUERY_STRING_KEY, defaultString(request.getQueryString()),
                USER_AGENT_KEY, defaultString(request.getHeader("User-Agent"))
        ));
    }

    public static Optional<Object> get(String key) {
        return TransmittableContextUtils.get(key);
    }

    public static void put(String key, Object value) {
        TransmittableContextUtils.put(key, value);
    }

    public static Map<String, Object> snapshot() {
        return TransmittableContextUtils.snapshot();
    }

    public static String getTraceId() {
        return TransmittableContextUtils.getString(TRACE_ID_KEY);
    }

    public static String getClientIp() {
        return blankToNull(TransmittableContextUtils.getString(CLIENT_IP_KEY));
    }

    public static String getRequestMethod() {
        return blankToNull(TransmittableContextUtils.getString(REQUEST_METHOD_KEY));
    }

    public static String getRequestUri() {
        return blankToNull(TransmittableContextUtils.getString(REQUEST_URI_KEY));
    }

    public static String getQueryString() {
        return blankToNull(TransmittableContextUtils.getString(QUERY_STRING_KEY));
    }

    public static String getUserAgent() {
        return blankToNull(TransmittableContextUtils.getString(USER_AGENT_KEY));
    }

    public static void clear() {
        TransmittableContextUtils.clear();
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
