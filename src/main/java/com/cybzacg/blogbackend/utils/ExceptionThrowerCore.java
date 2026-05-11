package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultCode;
import com.cybzacg.blogbackend.exception.BusinessException;

import java.util.function.Supplier;

/**
 * 异常抛出核心方法类。
 *
 * <p>约定：
 * 1) 业务异常统一抛 {@link BusinessException}
 * 2) 优先使用 {@link ResultCode} 作为错误码来源
 */
public class ExceptionThrowerCore {
    private ExceptionThrowerCore() {
        throw new IllegalStateException("Utility class");
    }

    // ===================== base =====================

    public static <E extends RuntimeException> void throwEx(E exception) {
        throw exception;
    }

    public static <E extends RuntimeException> void throwEx(Supplier<E> exceptionSupplier) {
        throw exceptionSupplier.get();
    }

    public static <E extends RuntimeException> void throwIf(boolean condition, E exception) {
        if (condition) {
            throwEx(exception);
        }
    }

    public static <E extends RuntimeException> void throwIf(boolean condition, Supplier<E> exceptionSupplier) {
        if (condition) {
            throwEx(exceptionSupplier);
        }
    }

    // ===================== business =====================

    public static void throwBusinessEx(ResultCode resultCode) {
        throw new BusinessException(resultCode);
    }

    /**
     * 抛出业务异常并声明返回值类型，适用于需要返回表达式但实际只会抛异常的分支。
     */
    public static <T> T throwBusiness(ResultCode resultCode) {
        throw new BusinessException(resultCode);
    }

    public static void throwBusinessEx(ResultCode resultCode, String message) {
        if (resultCode == null) {
            throw new BusinessException(500, message);
        }
        throw new BusinessException(resultCode.getCode(), message);
    }

    /**
     * 抛出带自定义消息的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(ResultCode resultCode, String message) {
        if (resultCode == null) {
            throw new BusinessException(500, message);
        }
        throw new BusinessException(resultCode.getCode(), message);
    }

    public static void throwBusinessEx(ResultCode resultCode, Throwable throwable) {
        if (resultCode == null) {
            throw new BusinessException("业务异常", throwable);
        }
        throw new BusinessException(resultCode.getCode(), resultCode.getMessage(), throwable);
    }

    /**
     * 抛出带原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(ResultCode resultCode, Throwable throwable) {
        if (resultCode == null) {
            throw new BusinessException("业务异常", throwable);
        }
        throw new BusinessException(resultCode.getCode(), resultCode.getMessage(), throwable);
    }

    public static void throwBusinessEx(ResultCode resultCode, String message, Throwable throwable) {
        if (resultCode == null) {
            throw new BusinessException(500, message, throwable);
        }
        throw new BusinessException(resultCode.getCode(), message, throwable);
    }

    /**
     * 抛出带自定义消息和原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(ResultCode resultCode, String message, Throwable throwable) {
        if (resultCode == null) {
            throw new BusinessException(500, message, throwable);
        }
        throw new BusinessException(resultCode.getCode(), message, throwable);
    }

    public static void throwBusinessEx(Integer code, String message) {
        throw new BusinessException(code, message);
    }

    /**
     * 抛出自定义错误码业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(Integer code, String message) {
        throw new BusinessException(code, message);
    }

    public static void throwBusinessEx(Integer code, String message, Throwable throwable) {
        throw new BusinessException(code, message, throwable);
    }

    /**
     * 抛出自定义错误码和原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(Integer code, String message, Throwable throwable) {
        throw new BusinessException(code, message, throwable);
    }

    public static void throwBusinessEx(String message) {
        throw new BusinessException(message);
    }

    /**
     * 抛出业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(String message) {
        throw new BusinessException(message);
    }

    public static void throwBusinessEx(String message, Throwable throwable) {
        throw new BusinessException(message, throwable);
    }

    /**
     * 抛出带原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(String message, Throwable throwable) {
        throw new BusinessException(message, throwable);
    }

    public static void throwBusinessIf(boolean condition, ResultCode resultCode) {
        if (condition) {
            throwBusinessEx(resultCode);
        }
    }

    public static void throwBusinessIf(boolean condition, ResultCode resultCode, String message) {
        if (condition) {
            throwBusinessEx(resultCode, message);
        }
    }

    public static void throwBusinessIfNot(boolean condition, ResultCode resultCode) {
        throwBusinessIf(!condition, resultCode);
    }

    public static void throwBusinessIfNot(boolean condition, ResultCode resultCode, String message) {
        throwBusinessIf(!condition, resultCode, message);
    }

    public static void throwBusinessIfNull(Object obj, ResultCode resultCode) {
        throwBusinessIf(obj == null, resultCode);
    }

    public static void throwBusinessIfNull(Object obj, ResultCode resultCode, String message) {
        throwBusinessIf(obj == null, resultCode, message);
    }

    public static void throwBusinessIfNotNull(Object obj, ResultCode resultCode) {
        throwBusinessIf(obj != null, resultCode);
    }

    public static void throwBusinessIfNotNull(Object obj, ResultCode resultCode, String message) {
        throwBusinessIf(obj != null, resultCode, message);
    }

    public static void throwBusinessIfBlank(String value, ResultCode resultCode) {
        throwBusinessIf(!StrUtils.hasText(value), resultCode);
    }

    public static void throwBusinessIfBlank(String value, ResultCode resultCode, String message) {
        throwBusinessIf(!StrUtils.hasText(value), resultCode, message);
    }

    public static <T> T requireNonNull(T obj, ResultCode resultCode) {
        throwBusinessIfNull(obj, resultCode);
        return obj;
    }

    public static String requireHasText(String value, ResultCode resultCode) {
        throwBusinessIfBlank(value, resultCode);
        return value;
    }

    public static <T> T require(Supplier<T> supplier, ResultCode resultCode) {
        T value = supplier.get();
        throwBusinessIfNull(value, resultCode);
        return value;
    }
}
