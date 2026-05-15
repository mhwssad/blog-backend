package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验文件大小是否超过配置的最大值。
 * 最大值来源于 {@code FileUploadProperties.maxFileSize}，null 视为合法。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = MaxUploadFileSizeValidator.class)
public @interface MaxUploadFileSize {
    String message() default "文件大小超出限制";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
