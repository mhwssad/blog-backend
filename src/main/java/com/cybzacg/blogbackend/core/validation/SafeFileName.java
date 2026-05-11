package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验文件名安全性：禁止路径分隔符、控制字符和双重扩展名。
 * null 或空字符串视为合法（由 @NotBlank 等注解控制必填）。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SafeFileNameValidator.class)
public @interface SafeFileName {
    String message() default "文件名包含非法字符或不允许双重扩展名";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
