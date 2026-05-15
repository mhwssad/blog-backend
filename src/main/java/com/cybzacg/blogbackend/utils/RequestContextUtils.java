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

    /**
     * 根据键名读取当前线程上下文中的字段值。
     *
     * @param key 上下文字段键名
     * @return 对应值的 {@link Optional}，键不存在时返回空
     */
    public static Optional<Object> get(String key) {
        return TransmittableContextUtils.get(key);
    }

    /**
     * 向当前线程上下文写入单个字段。
     *
     * @param key   上下文字段键名
     * @param value 待写入的值
     */
    public static void put(String key, Object value) {
        TransmittableContextUtils.put(key, value);
    }

    /**
     * 返回当前上下文的只读快照。
     *
     * @return 上下文中所有键值对的不可变映射
     */
    public static Map<String, Object> snapshot() {
        return TransmittableContextUtils.snapshot();
    }

    /**
     * 获取当前请求的链路追踪 ID。
     *
     * @return 追踪 ID，未绑定时返回 {@code null}
     */
    public static String getTraceId() {
        return TransmittableContextUtils.getString(TRACE_ID_KEY);
    }

    /**
     * 获取客户端 IP 地址。
     *
     * @return 客户端 IP，未绑定时返回 {@code null}
     */
    public static String getClientIp() {
        return blankToNull(TransmittableContextUtils.getString(CLIENT_IP_KEY));
    }

    /**
     * 获取当前请求的 HTTP 方法名（GET / POST / PUT 等）。
     *
     * @return HTTP 方法名，未绑定时返回 {@code null}
     */
    public static String getRequestMethod() {
        return blankToNull(TransmittableContextUtils.getString(REQUEST_METHOD_KEY));
    }

    /**
     * 获取当前请求的 URI 路径。
     *
     * @return 请求 URI，未绑定时返回 {@code null}
     */
    public static String getRequestUri() {
        return blankToNull(TransmittableContextUtils.getString(REQUEST_URI_KEY));
    }

    /**
     * 获取当前请求的查询字符串。
     *
     * @return 查询字符串，未绑定时返回 {@code null}
     */
    public static String getQueryString() {
        return blankToNull(TransmittableContextUtils.getString(QUERY_STRING_KEY));
    }

    /**
     * 获取当前请求的 User-Agent 头信息。
     *
     * @return User-Agent 字符串，未绑定时返回 {@code null}
     */
    public static String getUserAgent() {
        return blankToNull(TransmittableContextUtils.getString(USER_AGENT_KEY));
    }

    /**
     * 清除当前线程的请求上下文，释放所有已绑定字段。
     */
    public static void clear() {
        TransmittableContextUtils.clear();
    }

    /**
     * 将 {@code null} 转为空字符串，非 {@code null} 原样返回。
     *
     * @param value 可能为 {@code null} 的字符串
     * @return 非 null 字符串
     */
    private static String defaultString(String value) {
        return StrUtils.nullToEmpty(value);
    }

    /**
     * 将空白字符串转为 {@code null}，非空白原样返回。
     *
     * @param value 可能为空白或 {@code null} 的字符串
     * @return 有实际内容时返回原值，否则返回 {@code null}
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
