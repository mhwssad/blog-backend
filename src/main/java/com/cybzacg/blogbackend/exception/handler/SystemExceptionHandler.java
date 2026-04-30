package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeParseException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 系统级异常处理器。<p>兜底处理所有未被更高优先级处理器捕获的异常，包括空指针、IO、并发、反射、超时等 JVM 和框架级错误。</p>
 */
@Slf4j
@Order(6)
@RestControllerAdvice
public class SystemExceptionHandler extends BaseExceptionHandler {
    /**
     * 兜底处理 BusinessException，在业务处理器未捕获时提供带位置信息的日志记录。
     *
     * @param e 业务异常
     * @return 包含错误码和错误信息的统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        log.warn("业务异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(
                e.getCode(),
                e.getMessage(),
                "production".equals(profile) ? null
                        : e.getCause() != null ? e.getCause().getMessage() : null);
    }

    /**
     * 处理并发修改异常。
     *
     * @param e 并发修改异常
     * @return 并发冲突错误响应
     */
    @ExceptionHandler(ConcurrentModificationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Object> handleConcurrentModification(ConcurrentModificationException e) {
        log.error("并发修改异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.CONCURRENT_MODIFICATION,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理空指针异常。
     *
     * @param e 空指针异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.NULL_POINTER_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理类型转换异常。
     *
     * @param e 类型转换异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(ClassCastException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleClassCastException(ClassCastException e) {
        log.error("类型转换异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.CLASS_CAST_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理数组越界异常。
     *
     * @param e 数组越界异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleArrayIndexOutOfBounds(ArrayIndexOutOfBoundsException e) {
        log.error("数组越界异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.ARRAY_INDEX_OUT_OF_BOUNDS,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理算术异常。
     *
     * @param e 算术异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(ArithmeticException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleArithmeticException(ArithmeticException e) {
        log.error("算术异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.ARITHMETIC_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理非法参数异常。
     *
     * @param e 非法参数异常
     * @return 请求错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.ILLEGAL_ARGUMENT,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理非法状态异常。
     *
     * @param e 非法状态异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleIllegalStateException(IllegalStateException e) {
        log.error("非法状态异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.ILLEGAL_STATE,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理 IO 异常，区分客户端断开、SSE 请求和普通文件 IO 场景。
     *
     * @param e IO 异常
     * @return IO 错误响应，客户端断开或 SSE 请求时返回 null
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleIOException(IOException e) {
        if (isClientAbortException(e)) {
            log.debug("客户端断开连接 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage());
            return null;
        }

        if (isSseRequest()) {
            log.error("SSE请求IO异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
            handleSseErrorResponse("IO异常: " + e.getMessage());
            return null;
        }

        log.error("文件IO异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.IO_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理 JSON 处理异常。
     *
     * @param e JSON 处理异常
     * @return 请求错误响应
     */
    @ExceptionHandler(JsonProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleJsonProcessingException(JsonProcessingException e) {
        log.error("JSON处理异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.JSON_PROCESSING_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理日期格式解析异常。
     *
     * @param e 日期解析异常
     * @return 请求错误响应
     */
    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleDateTimeParseException(DateTimeParseException e) {
        log.error("日期格式异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DATE_FORMAT_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理数字格式解析异常。
     *
     * @param e 数字格式异常
     * @return 请求错误响应
     */
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleNumberFormatException(NumberFormatException e) {
        log.error("数字格式异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.NUMBER_FORMAT_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理不支持的操作异常。
     *
     * @param e 不支持操作异常
     * @return 未实现响应
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Result<Object> handleUnsupportedOperation(UnsupportedOperationException e) {
        log.error("不支持的操作 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.UNSUPPORTED_OPERATION,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理类未找到异常。
     *
     * @param e 类未找到异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(ClassNotFoundException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleClassNotFound(ClassNotFoundException e) {
        log.error("类未找到异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.CLASS_NOT_FOUND,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理方法未找到异常。
     *
     * @param e 方法未找到异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(NoSuchMethodException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleNoSuchMethod(NoSuchMethodException e) {
        log.error("方法未找到异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.NO_SUCH_METHOD,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理字段未找到异常。
     *
     * @param e 字段未找到异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(NoSuchFieldException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleNoSuchField(NoSuchFieldException e) {
        log.error("字段未找到异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.NO_SUCH_FIELD,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理非法访问异常。
     *
     * @param e 非法访问异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(IllegalAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleIllegalAccess(IllegalAccessException e) {
        log.error("非法访问异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.ILLEGAL_ACCESS,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理实例化异常。
     *
     * @param e 实例化异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(InstantiationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleInstantiationException(InstantiationException e) {
        log.error("实例化异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.INSTANTIATION_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理反射调用目标异常。
     *
     * @param e 反射调用异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(InvocationTargetException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleInvocationTargetException(InvocationTargetException e) {
        log.error("反射调用异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.INVOCATION_TARGET_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理线程中断异常，恢复中断状态后返回错误响应。
     *
     * @param e 线程中断异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(InterruptedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleInterruptedException(InterruptedException e) {
        log.error("线程中断异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        Thread.currentThread().interrupt();
        return buildErrorResult(ResultErrorCode.THREAD_INTERRUPTED,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理异步执行异常。
     *
     * @param e 执行异常
     * @return 内部服务器错误响应
     */
    @ExceptionHandler(ExecutionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleExecutionException(ExecutionException e) {
        log.error("执行异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.EXECUTION_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 处理超时异常。
     *
     * @param e 超时异常
     * @return 请求超时响应
     */
    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public Result<Object> handleTimeoutException(TimeoutException e) {
        log.error("超时异常 [TraceID: {}] [位置: {}] - {}", getTraceId(), getErrorLocation(e), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.TIMEOUT_ERROR,
                "production".equals(profile) ? null : buildErrorDetail(e));
    }

    /**
     * 全局兜底异常处理器，捕获所有未被其他处理器匹配的异常。
     *
     * @param e 未被捕获的异常
     * @return 系统错误响应，客户端断开或 SSE 请求时返回 null
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Object> handleGlobalException(Exception e) {
        if (isClientAbortException(e)) {
            log.debug("客户端断开连接 [TraceID: {}] - {}", getTraceId(), e.getMessage());
            return null;
        }

        if (isSseRequest()) {
            log.error("系统异常(SSE) [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
            Object sseDetail = "production".equals(profile) ? null : buildErrorDetail(e);
            String sseMessage = "production".equals(profile) ? ResultErrorCode.SYSTEM_ERROR.getMessage() : e.getMessage();
            handleSseErrorResponse(sseMessage, sseDetail);
            return null;
        }

        log.error("系统异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);

        String errorMessage = "production".equals(profile)
                ? ResultErrorCode.SYSTEM_ERROR.getMessage()
                : e.getMessage();
        Object errorDetail = "production".equals(profile) ? null : buildErrorDetail(e);

        return buildErrorResult(ResultErrorCode.SYSTEM_ERROR, errorMessage, errorDetail);
    }
}
