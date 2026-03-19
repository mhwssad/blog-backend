package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.exception.BusinessException;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务异常处理器
 */
@Order(2)
@RestControllerAdvice
public class BusinessExceptionHandler extends BaseExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        logException(e, "业务异常");
        return buildErrorResult(e.getCode(), e.getMessage(), null);
    }
}
