package com.cybzacg.blogbackend.common.constant;

/**
 * HTTP 请求头常量。<p>定义鉴权、链路追踪和客户端 IP 相关的 Header 名称。
 */
public final class HttpHeaderConstants {
    public static final String AUTHORIZATION = "Authorization";
    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String X_FORWARDED_FOR = "x-forwarded-for";
    public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    public static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
    public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";
    public static final String USER_AGENT = "User-Agent";
    public static final String UNKNOWN = "unknown";

    private HttpHeaderConstants() {
    }
}
