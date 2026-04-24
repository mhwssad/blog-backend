package com.cybzacg.blogbackend.exception;

import com.cybzacg.blogbackend.enums.error.ResultCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用业务异常。<p>携带错误码和错误信息，用于在业务逻辑中抛出可预期的错误，由全局异常处理器统一捕获并返回给前端。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String message;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BusinessException(String resultCode) {
        super(resultCode);
        this.code = 500;
        this.message = resultCode;
    }

    public BusinessException(String resultCode, Throwable cause) {
        super(resultCode, cause);
        this.code = 500;
        this.message = resultCode;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}
