package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Arrays;

/**
 * Spring MVC 异常处理器
 */
@Slf4j
@Order(1)
@RestControllerAdvice
public class SpringMvcExceptionHandler extends BaseExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("HTTP 消息不可读异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);

        String message = "请求参数格式错误";
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            message = "请求参数格式错误: " + e.getCause().getMessage();
        }

        return buildErrorResult(ResultErrorCode.HTTP_MESSAGE_NOT_READABLE, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.error("不支持的 HTTP 方法 [TraceID: {}] [位置: {}] - {} - 支持的方法: {}",
                getTraceId(), getErrorLocation(e), e.getMethod(), e.getSupportedMethods(), e);

        String message = String.format("不支持的 HTTP 方法: %s，支持的方法: %s",
                e.getMethod(), Arrays.toString(e.getSupportedMethods()));

        return buildErrorResult(ResultErrorCode.HTTP_METHOD_NOT_SUPPORTED, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.error("不支持的媒体类型 [TraceID: {}] [位置: {}] - {} - 支持的类型: {}",
                getTraceId(), getErrorLocation(e), e.getContentType(), e.getSupportedMediaTypes(), e);

        String message = String.format("不支持的媒体类型: %s，支持的类型: %s",
                e.getContentType(), e.getSupportedMediaTypes());

        return buildErrorResult(ResultErrorCode.HTTP_MEDIA_TYPE_NOT_SUPPORTED, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.error("缺少请求参数 [TraceID: {}] [位置: {}] - 参数名: {}, 类型: {}",
                getTraceId(), getErrorLocation(e), e.getParameterName(), e.getParameterType(), e);

        String message = String.format("缺少必填参数: %s", e.getParameterName());
        return buildErrorResult(ResultErrorCode.MISSING_REQUEST_PARAMETER, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingRequestHeader(MissingRequestHeaderException e) {
        log.error("缺少请求头 [TraceID: {}] [位置: {}] - 请求头名: {}",
                getTraceId(), getErrorLocation(e), e.getHeaderName(), e);

        String message = String.format("缺少必填请求头: %s", e.getHeaderName());
        return buildErrorResult(ResultErrorCode.MISSING_REQUEST_HEADER, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingPathVariable(MissingPathVariableException e) {
        log.error("缺少路径变量 [TraceID: {}] [位置: {}] - 变量名: {}",
                getTraceId(), getErrorLocation(e), e.getVariableName(), e);

        String message = String.format("缺少路径变量: %s", e.getVariableName());
        return buildErrorResult(ResultErrorCode.MISSING_PATH_VARIABLE, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingServletRequestPart(MissingServletRequestPartException e) {
        log.error("缺少请求部分 [TraceID: {}] [位置: {}] - 部分名: {}",
                getTraceId(), getErrorLocation(e), e.getRequestPartName(), e);

        String message = String.format("缺少必填文件: %s", e.getRequestPartName());
        return buildErrorResult(ResultErrorCode.MISSING_REQUEST_PART, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.error("文件大小超出限制 [TraceID: {}] [位置: {}] - {}",
                getTraceId(), getErrorLocation(e), e.getMessage(), e);

        String message = "上传文件大小超出限制";
        if (e.getMaxUploadSize() != -1) {
            long maxSizeMB = e.getMaxUploadSize() / 1024 / 1024;
            message = String.format("上传文件大小超出限制，最大允许: %d MB", maxSizeMB);
        }

        return buildErrorResult(ResultErrorCode.MAX_UPLOAD_SIZE_EXCEEDED, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Object> handleNoHandlerFound(NoHandlerFoundException e) {
        log.error("没有找到请求处理器 [TraceID: {}] [位置: {}] - {} {}",
                getTraceId(), getErrorLocation(e), e.getHttpMethod(), e.getRequestURL(), e);

        String message = String.format("请求的接口不存在: %s %s", e.getHttpMethod(), e.getRequestURL());
        return buildErrorResult(ResultErrorCode.NO_HANDLER_FOUND, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }
}
