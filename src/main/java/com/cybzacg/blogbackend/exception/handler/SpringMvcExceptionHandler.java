package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
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
 * Spring MVC 异常处理器。<p>处理 HTTP 协议级别的异常，包括消息不可读、方法不支持、缺少参数/路径变量/请求头、文件上传超限和 404。</p>
 */
@Slf4j
@Order(1)
@RestControllerAdvice
public class SpringMvcExceptionHandler extends BaseExceptionHandler {
    /**
     * 处理 HTTP 消息不可读异常（请求体解析失败）。
     *
     * @param e 消息不可读异常
     * @return 包含格式错误提示的统一响应
     */
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

    /**
     * 处理 HTTP 方法不支持异常。
     *
     * @param e 方法不支持异常
     * @return 包含支持方法列表的统一响应
     */
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

    /**
     * 处理媒体类型不支持异常。
     *
     * @param e 媒体类型不支持异常
     * @return 包含支持媒体类型的统一响应
     */
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

    /**
     * 处理缺少 Servlet 请求参数异常。
     *
     * @param e 缺少参数异常
     * @return 包含缺失参数名的统一响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.error("缺少请求参数 [TraceID: {}] [位置: {}] - 参数名: {}, 类型: {}",
                getTraceId(), getErrorLocation(e), e.getParameterName(), e.getParameterType(), e);

        String message = String.format("缺少必填参数: %s", e.getParameterName());
        return buildErrorResult(ResultErrorCode.MISSING_REQUEST_PARAMETER, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理缺少请求头异常。
     *
     * @param e 缺少请求头异常
     * @return 包含缺失请求头名的统一响应
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingRequestHeader(MissingRequestHeaderException e) {
        log.error("缺少请求头 [TraceID: {}] [位置: {}] - 请求头名: {}",
                getTraceId(), getErrorLocation(e), e.getHeaderName(), e);

        String message = String.format("缺少必填请求头: %s", e.getHeaderName());
        return buildErrorResult(ResultErrorCode.MISSING_REQUEST_HEADER, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理缺少路径变量异常。
     *
     * @param e 缺少路径变量异常
     * @return 包含缺失变量名的统一响应
     */
    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingPathVariable(MissingPathVariableException e) {
        log.error("缺少路径变量 [TraceID: {}] [位置: {}] - 变量名: {}",
                getTraceId(), getErrorLocation(e), e.getVariableName(), e);

        String message = String.format("缺少路径变量: %s", e.getVariableName());
        return buildErrorResult(ResultErrorCode.MISSING_PATH_VARIABLE, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理缺少请求部分（如文件上传字段）异常。
     *
     * @param e 缺少请求部分异常
     * @return 包含缺失部分名的统一响应
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingServletRequestPart(MissingServletRequestPartException e) {
        log.error("缺少请求部分 [TraceID: {}] [位置: {}] - 部分名: {}",
                getTraceId(), getErrorLocation(e), e.getRequestPartName(), e);

        String message = String.format("缺少必填文件: %s", e.getRequestPartName());
        return buildErrorResult(ResultErrorCode.MISSING_REQUEST_PART, message,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理文件上传大小超限异常。
     *
     * @param e 上传超限异常
     * @return 包含最大允许大小的统一响应
     */
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

    /**
     * 处理请求处理器未找到异常（404）。
     *
     * @param e 处理器未找到异常
     * @return 包含请求路径的 404 响应
     */
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
