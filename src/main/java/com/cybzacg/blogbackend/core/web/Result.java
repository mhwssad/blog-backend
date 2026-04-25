package com.cybzacg.blogbackend.core.web;

import com.cybzacg.blogbackend.enums.error.ResultCode;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应结构体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {
    private Integer code;

    private T data;

    private String message;

    /**
     * 响应时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> Result<T> of(Integer code, String msg, T data) {
        return Result.<T>builder()
                .code(code)
                .message(msg)
                .data(data)
                .build();
    }

    public static <T> Result<T> of(ResultCode resultCode, T data) {
        return Result.<T>builder()
                .code(resultCode.getCode())
                .message(resultCode.getMessage())
                .data(data)
                .build();
    }

    /**
     * 成功响应（默认）
     */
    public static <T> Result<T> success() {
        return of(ResultErrorCode.SUCCESS, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return of(ResultErrorCode.SUCCESS, data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return of(ResultErrorCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 业务失败响应
     */
    public static <T> Result<T> fail() {
        return of(ResultErrorCode.FAIL, null);
    }

    /**
     * 业务失败响应（自定义消息）
     */
    public static <T> Result<T> fail(String message) {
        return of(ResultErrorCode.FAIL.getCode(), message, null);
    }

    /**
     * 根据ResultCode创建错误响应
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return of(resultCode, null);
    }

    /**
     * 根据条件返回成功或失败响应
     */
    public static <T> Result<T> condition(boolean success) {
        return success ? success() : fail();
    }

    public static <T> Result<T> condition(boolean success, T data) {
        return success ? success(data) : fail("操作失败");
    }

    public static <T> Result<T> condition(boolean success, String successMessage,
                                          String failMessage, T data) {
        return success ? success(successMessage, data) : fail(failMessage);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultErrorCode.SUCCESS.getCode().equals(this.code);
    }

}
