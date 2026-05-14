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

    /**
     * 无条件抛出给定的运行时异常。
     *
     * @param exception 要抛出的运行时异常
     * @throws E 始终抛出给定的异常
     */
    public static <E extends RuntimeException> void throwEx(E exception) {
        throw exception;
    }

    /**
     * 无条件抛出由供应商提供的运行时异常。
     *
     * @param exceptionSupplier 异常供应商，不允许为 null
     * @throws E 始终抛出供应商提供的异常
     */
    public static <E extends RuntimeException> void throwEx(
        Supplier<E> exceptionSupplier
    ) {
        throw exceptionSupplier.get();
    }

    /**
     * 条件为 true 时抛出给定的运行时异常。
     *
     * @param condition 判断条件
     * @param exception 条件成立时要抛出的异常
     * @throws E 当 condition 为 true 时抛出
     */
    public static <E extends RuntimeException> void throwIf(
        boolean condition,
        E exception
    ) {
        if (condition) {
            throwEx(exception);
        }
    }

    /**
     * 条件为 true 时抛出由供应商提供的运行时异常。
     *
     * @param condition       判断条件
     * @param exceptionSupplier 异常供应商，仅在条件成立时调用
     * @throws E 当 condition 为 true 时抛出
     */
    public static <E extends RuntimeException> void throwIf(
        boolean condition,
        Supplier<E> exceptionSupplier
    ) {
        if (condition) {
            throwEx(exceptionSupplier);
        }
    }

    // ===================== business =====================

    /**
     * 无条件抛出带结果码的业务异常。
     *
     * @param resultCode 错误结果码
     * @throws BusinessException 始终抛出
     */
    public static void throwBusinessEx(ResultCode resultCode) {
        throw new BusinessException(resultCode);
    }

    /**
     * 抛出业务异常并声明返回值类型，适用于需要返回表达式但实际只会抛异常的分支。
     */
    public static <T> T throwBusiness(ResultCode resultCode) {
        throw new BusinessException(resultCode);
    }

    /**
     * 无条件抛出带结果码和自定义消息的业务异常；resultCode 为 null 时回退到 500。
     *
     * @param resultCode 错误结果码，可为 null
     * @param message    自定义错误消息
     * @throws BusinessException 始终抛出
     */
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

    /**
     * 无条件抛出带结果码和原因的业务异常；resultCode 为 null 时使用默认消息。
     *
     * @param resultCode 错误结果码，可为 null
     * @param throwable  异常原因
     * @throws BusinessException 始终抛出
     */
    public static void throwBusinessEx(
        ResultCode resultCode,
        Throwable throwable
    ) {
        if (resultCode == null) {
            throw new BusinessException("业务异常", throwable);
        }
        throw new BusinessException(
            resultCode.getCode(),
            resultCode.getMessage(),
            throwable
        );
    }

    /**
     * 抛出带原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(
        ResultCode resultCode,
        Throwable throwable
    ) {
        if (resultCode == null) {
            throw new BusinessException("业务异常", throwable);
        }
        throw new BusinessException(
            resultCode.getCode(),
            resultCode.getMessage(),
            throwable
        );
    }

    /**
     * 无条件抛出带结果码、自定义消息和原因的业务异常；resultCode 为 null 时回退到 500。
     *
     * @param resultCode 错误结果码，可为 null
     * @param message    自定义错误消息
     * @param throwable  异常原因
     * @throws BusinessException 始终抛出
     */
    public static void throwBusinessEx(
        ResultCode resultCode,
        String message,
        Throwable throwable
    ) {
        if (resultCode == null) {
            throw new BusinessException(500, message, throwable);
        }
        throw new BusinessException(resultCode.getCode(), message, throwable);
    }

    /**
     * 抛出带自定义消息和原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(
        ResultCode resultCode,
        String message,
        Throwable throwable
    ) {
        if (resultCode == null) {
            throw new BusinessException(500, message, throwable);
        }
        throw new BusinessException(resultCode.getCode(), message, throwable);
    }

    /**
     * 无条件抛出带整数错误码和消息的业务异常。
     *
     * @param code    错误码
     * @param message 错误消息
     * @throws BusinessException 始终抛出
     */
    public static void throwBusinessEx(Integer code, String message) {
        throw new BusinessException(code, message);
    }

    /**
     * 抛出自定义错误码业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(Integer code, String message) {
        throw new BusinessException(code, message);
    }

    /**
     * 无条件抛出带整数错误码、消息和原因的业务异常。
     *
     * @param code      错误码
     * @param message   错误消息
     * @param throwable 异常原因
     * @throws BusinessException 始终抛出
     */
    public static void throwBusinessEx(
        Integer code,
        String message,
        Throwable throwable
    ) {
        throw new BusinessException(code, message, throwable);
    }

    /**
     * 抛出自定义错误码和原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(
        Integer code,
        String message,
        Throwable throwable
    ) {
        throw new BusinessException(code, message, throwable);
    }

    /**
     * 无条件抛出带自定义消息的业务异常。
     *
     * @param message 错误消息
     * @throws BusinessException 始终抛出
     */
    public static void throwBusinessEx(String message) {
        throw new BusinessException(message);
    }

    /**
     * 抛出业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(String message) {
        throw new BusinessException(message);
    }

    /**
     * 无条件抛出带自定义消息和原因的业务异常。
     *
     * @param message   错误消息
     * @param throwable 异常原因
     * @throws BusinessException 始终抛出
     */
    public static void throwBusinessEx(String message, Throwable throwable) {
        throw new BusinessException(message, throwable);
    }

    /**
     * 抛出带原因的业务异常并声明返回值类型。
     */
    public static <T> T throwBusiness(String message, Throwable throwable) {
        throw new BusinessException(message, throwable);
    }

    /**
     * 条件为 true 时抛出带结果码的业务异常。
     *
     * @param condition  判断条件
     * @param resultCode 错误结果码
     * @throws BusinessException 当 condition 为 true 时抛出
     */
    public static void throwBusinessIf(
        boolean condition,
        ResultCode resultCode
    ) {
        if (condition) {
            throwBusinessEx(resultCode);
        }
    }

    /**
     * 条件为 true 时抛出带结果码和自定义消息的业务异常。
     *
     * @param condition  判断条件
     * @param resultCode 错误结果码，可为 null
     * @param message    自定义错误消息
     * @throws BusinessException 当 condition 为 true 时抛出
     */
    public static void throwBusinessIf(
        boolean condition,
        ResultCode resultCode,
        String message
    ) {
        if (condition) {
            throwBusinessEx(resultCode, message);
        }
    }

    /**
     * 条件为 false 时抛出带结果码的业务异常。
     *
     * @param condition  判断条件
     * @param resultCode 错误结果码
     * @throws BusinessException 当 condition 为 false 时抛出
     */
    public static void throwBusinessIfNot(
        boolean condition,
        ResultCode resultCode
    ) {
        throwBusinessIf(!condition, resultCode);
    }

    /**
     * 条件为 false 时抛出带结果码和自定义消息的业务异常。
     *
     * @param condition  判断条件
     * @param resultCode 错误结果码，可为 null
     * @param message    自定义错误消息
     * @throws BusinessException 当 condition 为 false 时抛出
     */
    public static void throwBusinessIfNot(
        boolean condition,
        ResultCode resultCode,
        String message
    ) {
        throwBusinessIf(!condition, resultCode, message);
    }

    /**
     * 对象为 null 时抛出业务异常。
     *
     * @param obj        待检查对象
     * @param resultCode 错误结果码
     * @throws BusinessException 当 obj 为 null 时抛出
     */
    public static void throwBusinessIfNull(Object obj, ResultCode resultCode) {
        throwBusinessIf(obj == null, resultCode);
    }

    /**
     * 对象为 null 时抛出带自定义消息的业务异常。
     *
     * @param obj        待检查对象
     * @param resultCode 错误结果码，可为 null
     * @param message    自定义错误消息
     * @throws BusinessException 当 obj 为 null 时抛出
     */
    public static void throwBusinessIfNull(
        Object obj,
        ResultCode resultCode,
        String message
    ) {
        throwBusinessIf(obj == null, resultCode, message);
    }

    /**
     * 对象不为 null 时抛出业务异常。
     *
     * @param obj        待检查对象
     * @param resultCode 错误结果码
     * @throws BusinessException 当 obj 不为 null 时抛出
     */
    public static void throwBusinessIfNotNull(
        Object obj,
        ResultCode resultCode
    ) {
        throwBusinessIf(obj != null, resultCode);
    }

    /**
     * 对象不为 null 时抛出带自定义消息的业务异常。
     *
     * @param obj        待检查对象
     * @param resultCode 错误结果码，可为 null
     * @param message    自定义错误消息
     * @throws BusinessException 当 obj 不为 null 时抛出
     */
    public static void throwBusinessIfNotNull(
        Object obj,
        ResultCode resultCode,
        String message
    ) {
        throwBusinessIf(obj != null, resultCode, message);
    }

    /**
     * 字符串为空白（null 或无可见字符）时抛出业务异常。
     *
     * @param value      待检查字符串
     * @param resultCode 错误结果码
     * @throws BusinessException 当 value 为空白时抛出
     */
    public static void throwBusinessIfBlank(
        String value,
        ResultCode resultCode
    ) {
        throwBusinessIf(!StrUtils.hasText(value), resultCode);
    }

    /**
     * 字符串为空白（null 或无可见字符）时抛出带自定义消息的业务异常。
     *
     * @param value      待检查字符串
     * @param resultCode 错误结果码，可为 null
     * @param message    自定义错误消息
     * @throws BusinessException 当 value 为空白时抛出
     */
    public static void throwBusinessIfBlank(
        String value,
        ResultCode resultCode,
        String message
    ) {
        throwBusinessIf(!StrUtils.hasText(value), resultCode, message);
    }

    /**
     * 要求对象非空，否则抛出业务异常；检查通过则返回该对象。
     *
     * @param obj        待检查对象
     * @param resultCode 错误结果码
     * @param <T>        对象类型
     * @return 传入的对象（非空）
     * @throws BusinessException 当 obj 为 null 时抛出
     */
    public static <T> T requireNonNull(T obj, ResultCode resultCode) {
        throwBusinessIfNull(obj, resultCode);
        return obj;
    }

    /**
     * 要求字符串非空白，否则抛出业务异常；检查通过则返回该字符串。
     *
     * @param value      待检查字符串
     * @param resultCode 错误结果码
     * @return 传入的字符串（非空白）
     * @throws BusinessException 当 value 为空白时抛出
     */
    public static String requireHasText(String value, ResultCode resultCode) {
        throwBusinessIfBlank(value, resultCode);
        return value;
    }

    /**
     * 通过供应商获取值并要求非空，否则抛出业务异常；检查通过则返回该值。
     *
     * @param supplier   值供应商
     * @param resultCode 错误结果码
     * @param <T>        值类型
     * @return 供应商提供的值（非空）
     * @throws BusinessException 当供应商返回 null 时抛出
     */
    public static <T> T require(Supplier<T> supplier, ResultCode resultCode) {
        T value = supplier.get();
        throwBusinessIfNull(value, resultCode);
        return value;
    }
}
