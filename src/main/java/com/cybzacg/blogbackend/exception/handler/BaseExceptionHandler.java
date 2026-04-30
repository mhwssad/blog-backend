package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.error.ResultCode;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 异常处理器基类。<p>提供统一的错误响应构建、日志记录、TraceId 获取、错误位置定位、客户端断开检测和 SSE 错误写入等公共能力，供各具体异常处理器继承。</p>
 */
public abstract class BaseExceptionHandler {
    protected static final Logger log = LoggerFactory.getLogger(BaseExceptionHandler.class);
    private static final String DEV_PROFILE = "dev";

    @Value("${spring.profiles.active:dev}")
    protected String profile;

    @Value("${spring.application.name:unknown}")
    protected String applicationName;

    /**
     * 获取当前请求的 ServletRequestAttributes。
     *
     * @return ServletRequestAttributes，无线程绑定上下文时返回 null
     */
    protected ServletRequestAttributes getRequestAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }

    /**
     * 获取当前 HTTP 请求对象。
     *
     * @return HttpServletRequest，不可用时返回 null
     */
    protected HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取当前 HTTP 响应对象。
     *
     * @return HttpServletResponse，不可用时返回 null
     */
    protected HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = getRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    /**
     * 获取当前请求的 TraceId，优先从上下文工具获取，回退到请求属性。
     *
     * @return TraceId 字符串，不可用时返回 null
     */
    protected String getTraceId() {
        String traceId = RequestContextUtils.getTraceId();
        if (traceId != null) {
            return traceId;
        }
        HttpServletRequest request = getRequest();
        return request != null ? (String) request.getAttribute(RequestContextUtils.TRACE_ID_ATTRIBUTE) : null;
    }

    /**
     * 记录异常日志，包含 TraceId 和错误位置；DEBUG 模式下输出完整堆栈。
     *
     * @param e       异常
     * @param message 日志前缀描述
     */
    protected void logException(Exception e, String message) {
        String errorLocation = getErrorLocation(e);
        if (log.isDebugEnabled()) {
            log.error("{} [TraceID: {}] [位置: {}] - {}", message, getTraceId(), errorLocation, e.getMessage(), e);
            return;
        }

        log.error("{} [TraceID: {}] [位置: {}] - {}", message, getTraceId(), errorLocation, e.getMessage());
    }

    /**
     * 获取异常的首帧位置信息（类名.方法名(文件:行号)）。
     *
     * @param e 异常
     * @return 格式化的位置字符串
     */
    protected String getErrorLocation(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            StackTraceElement element = stackTrace[0];
            return String.format("%s.%s(%s:%d)",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber());
        }
        return "未知位置";
    }

    /**
     * 根据错误码、消息和数据构建错误响应。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param data    附加数据
     * @return 统一错误响应
     */
    protected Result<Object> buildErrorResult(Integer code, String message, Object data) {
        return Result.of(code, message, data);
    }

    /**
     * 根据结果码和附加数据构建错误响应。
     *
     * @param resultCode 结果码枚举
     * @param data       附加数据
     * @return 统一错误响应
     */
    protected Result<Object> buildErrorResult(ResultCode resultCode, Object data) {
        return buildErrorResult(resultCode.getCode(), resultCode.getMessage(), data);
    }

    /**
     * 根据结果码、自定义消息和附加数据构建错误响应。
     *
     * @param resultCode 结果码枚举
     * @param message    自定义错误消息
     * @param data       附加数据
     * @return 统一错误响应
     */
    protected Result<Object> buildErrorResult(ResultCode resultCode, String message, Object data) {
        return buildErrorResult(resultCode.getCode(), message, data);
    }

    /**
     * 根据结果码和自定义消息构建错误响应。
     *
     * @param resultCode 结果码枚举
     * @param message    自定义错误消息
     * @return 统一错误响应
     */
    protected Result<Object> buildErrorResult(ResultCode resultCode, String message) {
        return buildErrorResult(resultCode, message, null);
    }

    /**
     * 根据结果码构建错误响应（无附加数据）。
     *
     * @param resultCode 结果码枚举
     * @return 统一错误响应
     */
    protected Result<Object> buildErrorResult(ResultCode resultCode) {
        return buildErrorResult(resultCode, null);
    }

    /**
     * 构建包含异常摘要、首帧位置、dev 环境堆栈和根因的错误详情映射。
     *
     * @param e 异常
     * @return 错误详情 Map
     */
    protected Map<String, Object> buildErrorDetail(Exception e) {
        Map<String, Object> errorDetail = new LinkedHashMap<>(buildThrowableSummary(e));

        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            errorDetail.put("location", buildLocation(stackTrace[0]));
        }

        if (DEV_PROFILE.equals(profile) && stackTrace != null) {
            List<Map<String, Object>> stackTraceList = Arrays.stream(stackTrace)
                    .limit(10)
                    .map(this::buildTraceElement)
                    .toList();
            errorDetail.put("stackTrace", stackTraceList);
        }

        Throwable cause = e.getCause();
        if (cause != null) {
            Map<String, Object> causeDetail = new LinkedHashMap<>(buildThrowableSummary(cause));
            if (cause.getStackTrace() != null && cause.getStackTrace().length > 0) {
                causeDetail.put("location", buildLocation(cause.getStackTrace()[0]));
            }
            errorDetail.put("cause", causeDetail);
        }

        return errorDetail;
    }

    private Map<String, Object> buildThrowableSummary(Throwable throwable) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("error", throwable.getClass().getName());
        detail.put("message", throwable.getMessage());
        return detail;
    }

    private Map<String, Object> buildLocation(StackTraceElement firstElement) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("className", firstElement.getClassName());
        location.put("methodName", firstElement.getMethodName());
        location.put("fileName", firstElement.getFileName());
        location.put("lineNumber", firstElement.getLineNumber());
        return location;
    }

    private Map<String, Object> buildTraceElement(StackTraceElement element) {
        Map<String, Object> traceElement = new LinkedHashMap<>();
        traceElement.put("class", element.getClassName());
        traceElement.put("method", element.getMethodName());
        traceElement.put("file", element.getFileName());
        traceElement.put("line", element.getLineNumber());
        return traceElement;
    }

    /**
     * 判断当前请求是否为 SSE（Server-Sent Events）请求。
     *
     * @return 是 SSE 请求返回 true
     */
    protected boolean isSseRequest() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return false;
        }

        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return false;
        }

        try {
            return MediaType.parseMediaTypes(acceptHeader).stream()
                    .anyMatch(mediaType -> mediaType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM));
        } catch (InvalidMediaTypeException ignored) {
            return acceptHeader.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
        }
    }

    /**
     * 判断是否为客户端断开连接异常（IO 层）。
     *
     * @param e IO 异常
     * @return 客户端断开返回 true
     */
    protected boolean isClientAbortException(IOException e) {
        return isClientAbortException((Throwable) e);
    }

    /**
     * 判断是否为客户端断开连接异常（Throwable 层），遍历异常链检测。
     *
     * @param e 异常
     * @return 客户端断开返回 true
     */
    protected boolean isClientAbortException(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String className = current.getClass().getName();
            if (className.contains("ClientAbortException")
                    || className.contains("AsyncRequestNotUsableException")
                    || (current.getMessage() != null
                    && current.getMessage().contains("你的主机中的软件中止了一个已建立的连接"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 向 SSE 连接写入错误事件响应。
     *
     * @param errorMessage 错误消息内容
     */
    protected void handleSseErrorResponse(String errorMessage) {
        handleSseErrorResponse(errorMessage, null);
    }

    /**
     * 向 SSE 连接写入错误事件响应（含附加数据）。
     *
     * @param errorMessage 错误消息内容
     * @param data         附加数据（如异常详情）
     */
    protected void handleSseErrorResponse(String errorMessage, Object data) {
        HttpServletResponse response = getResponse();
        if (response != null) {
            try {
                Result<Object> errorResult = buildErrorResult(ResultErrorCode.SYSTEM_ERROR, errorMessage, data);
                String errorJson = JsonUtils.toJson(errorResult);

                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType("text/event-stream;charset=UTF-8");
                response.getWriter().write("event: error\ndata: " + errorJson + "\n\n");
                response.getWriter().flush();
            } catch (IOException ioException) {
                log.error("Failed to write SSE error response", ioException);
            }
        }
    }
}
