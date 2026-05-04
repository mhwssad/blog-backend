package com.cybzacg.blogbackend.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ExceptionThrowerCoreTest {

    @Test
    void throwBusinessShouldSupportReturnExpression() {
        assertThrows(BusinessException.class, this::returnWithBusinessThrow);
    }

    private String returnWithBusinessThrow() {
        return ExceptionThrowerCore.throwBusiness(
            ResultErrorCode.PARAM_VALIDATION_FAILED,
            "测试返回表达式异常"
        );
    }
}
