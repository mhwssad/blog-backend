package com.cybzacg.blogbackend.core.util;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.error.ResultCode;
import com.cybzacg.blogbackend.utils.JsonUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HttpServletResponse 响应工具类
 */
public final class HttpServletResponseUtils {

    private HttpServletResponseUtils() {
    }

    public static void writeJson(HttpServletResponse response,
                                 int httpStatus,
                                 ResultCode resultCode) throws IOException {
        writeJson(response, httpStatus, resultCode.getCode(), resultCode.getMessage());
    }

    public static void writeJson(HttpServletResponse response,
                                 int httpStatus,
                                 Integer code,
                                 String message) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtils.toJson(Result.of(code, message, null)));
    }
}
