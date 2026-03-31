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
 * 异常处理器基类
 */
public abstract class BaseExceptionHandler {
    protected static final Logger log = LoggerFactory.getLogger(BaseExceptionHandler.class);
    private static final String DEV_PROFILE = "dev";

    @Value("${spring.profiles.active:dev}")
    protected String profile;

    @Value("${spring.application.name:unknown}")
    protected String applicationName;

    protected ServletRequestAttributes getRequestAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }

    protected HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    protected HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = getRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    protected String getTraceId() {
        String traceId = RequestContextUtils.getTraceId();
        if (traceId != null) {
            return traceId;
        }
        HttpServletRequest request = getRequest();
        return request != null ? (String) request.getAttribute(RequestContextUtils.TRACE_ID_ATTRIBUTE) : null;
    }

    protected void logException(Exception e, String message) {
        String errorLocation = getErrorLocation(e);
        if (log.isDebugEnabled()) {
            log.error("{} [TraceID: {}] [位置: {}] - {}", message, getTraceId(), errorLocation, e.getMessage(), e);
            return;
        }

        log.error("{} [TraceID: {}] [位置: {}] - {}", message, getTraceId(), errorLocation, e.getMessage());
    }

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

    protected Result<Object> buildErrorResult(Integer code, String message, Object data) {
        return Result.of(code, message, data);
    }

    protected Result<Object> buildErrorResult(ResultCode resultCode, Object data) {
        return buildErrorResult(resultCode.getCode(), resultCode.getMessage(), data);
    }

    protected Result<Object> buildErrorResult(ResultCode resultCode, String message, Object data) {
        return buildErrorResult(resultCode.getCode(), message, data);
    }

    protected Result<Object> buildErrorResult(ResultCode resultCode, String message) {
        return buildErrorResult(resultCode, message, null);
    }

    protected Result<Object> buildErrorResult(ResultCode resultCode) {
        return buildErrorResult(resultCode, null);
    }

    protected Map<String, Object> buildErrorDetail(Exception e) {
        Map<String, Object> errorDetail = new LinkedHashMap<>();
        errorDetail.putAll(buildThrowableSummary(e));

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

    protected boolean isClientAbortException(IOException e) {
        return isClientAbortException((Throwable) e);
    }

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

    protected void handleSseErrorResponse(String errorMessage) {
        HttpServletResponse response = getResponse();
        if (response != null) {
            try {
                Result<Object> errorResult = buildErrorResult(ResultErrorCode.SYSTEM_ERROR, errorMessage, null);
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
