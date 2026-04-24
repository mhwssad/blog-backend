package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 参数校验异常处理器。<p>统一处理请求参数绑定、约束违反、类型不匹配及消息格式转换等校验类异常。</p>
 */
@Slf4j
@Order(3)
@RestControllerAdvice
public class ValidationExceptionHandler extends BaseExceptionHandler {
    /**
     * 处理 @Valid 校验失败异常，提取字段级错误信息。
     *
     * @param e 方法参数校验异常
     * @return 包含字段错误详情的统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("参数校验失败 [TraceID: {}]", getTraceId());

        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        Map<String, String> errorMap = new LinkedHashMap<>();
        for (FieldError error : fieldErrors) {
            errorMap.put(error.getField(), error.getDefaultMessage());
        }

        String message = errorMap.size() == 1
                ? errorMap.values().iterator().next()
                : ResultErrorCode.PARAM_VALIDATION_FAILED.getMessage();

        return buildErrorResult(ResultErrorCode.PARAM_VALIDATION_FAILED, message,
                "production".equals(profile) ? null : errorMap);
    }

    /**
     * 处理对象绑定异常，提取字段和全局错误信息。
     *
     * @param e 绑定异常
     * @return 包含绑定错误详情的统一响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBindException(BindException e) {
        log.warn("对象绑定异常 [TraceID: {}]", getTraceId());

        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<ObjectError> globalErrors = e.getBindingResult().getGlobalErrors();
        Map<String, String> errorMap = new LinkedHashMap<>();

        for (FieldError error : fieldErrors) {
            errorMap.put(error.getField(), error.getDefaultMessage());
        }
        for (ObjectError error : globalErrors) {
            errorMap.put(error.getObjectName(), error.getDefaultMessage());
        }

        return buildErrorResult(ResultErrorCode.PARAM_BIND_ERROR,
                "production".equals(profile) ? null : errorMap);
    }

    /**
     * 处理 JSR-303 约束违反异常，提取属性路径和错误消息。
     *
     * @param e 约束违反异常
     * @return 包含约束错误详情的统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("约束违反异常 [TraceID: {}]", getTraceId());

        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        Map<String, String> errorMap = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                    : propertyPath;
            errorMap.put(fieldName, violation.getMessage());
        }

        String message = errorMap.size() == 1
                ? errorMap.values().iterator().next()
                : ResultErrorCode.PARAM_VALIDATION_FAILED.getMessage();

        return buildErrorResult(ResultErrorCode.PARAM_VALIDATION_FAILED, message,
                "production".equals(profile) ? null : errorMap);
    }

    /**
     * 处理方法参数类型不匹配异常。
     *
     * @param e 参数类型不匹配异常
     * @return 包含期望类型和实际值的统一响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("方法参数类型不匹配 [TraceID: {}]: {} - 期望类型: {}, 实际值: {}",
                getTraceId(), e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知",
                e.getValue());

        String error = String.format("参数 '%s' 类型错误，期望类型: %s",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");

        Map<String, Object> errorDetail = new LinkedHashMap<>();
        errorDetail.put("parameter", e.getName());
        errorDetail.put("expectedType", e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        errorDetail.put("actualValue", e.getValue());

        return buildErrorResult(ResultErrorCode.PARAM_TYPE_MISMATCH, error,
                "production".equals(profile) ? null : errorDetail);
    }

    /**
     * 处理通用类型不匹配异常。
     *
     * @param e 类型不匹配异常
     * @return 包含期望类型的统一响应
     */
    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleTypeMismatch(TypeMismatchException e) {
        log.warn("类型不匹配异常 [TraceID: {}]: {} - 期望类型: {}, 实际值: {}",
                getTraceId(), e.getPropertyName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知",
                e.getValue());

        String error = String.format("参数类型错误，期望类型: %s",
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");

        return buildErrorResult(ResultErrorCode.PARAM_TYPE_MISMATCH, error,
                "production".equals(profile) ? null : e.getValue());
    }

    /**
     * 处理 HTTP 消息格式转换异常（如 JSON/XML 格式错误）。
     *
     * @param e 消息转换异常
     * @return 包含格式错误提示的统一响应
     */
    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleHttpMessageConversion(HttpMessageConversionException e) {
        log.error("HTTP消息转换异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);

        String message = "数据格式转换失败";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("JSON")) {
                message = "JSON格式错误";
            } else if (e.getMessage().contains("XML")) {
                message = "XML格式错误";
            }
        }

        return buildErrorResult(ResultErrorCode.PARAM_FORMAT_INVALID, message,
                "production".equals(profile) ? null : e.getMessage());
    }
}
