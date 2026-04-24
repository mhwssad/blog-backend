package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.exception.BusinessException;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务异常处理器。<p>捕获 BusinessException，提取错误码和错误信息并封装为统一响应格式返回给前端。</p>
 */
@Order(2)
@RestControllerAdvice
public class BusinessExceptionHandler extends BaseExceptionHandler {
    /**
     * 处理 BusinessException，直接透传其错误码和错误信息。
     *
     * @param e 业务异常
     * @return 包含错误码和错误信息的统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        logException(e, "业务异常");
        return buildErrorResult(e.getCode(), e.getMessage(), null);
    }
}
