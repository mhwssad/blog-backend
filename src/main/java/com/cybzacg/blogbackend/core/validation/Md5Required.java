package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 当配置启用了 MD5 校验时，要求字段值不能为空。
 * 配置来源于 {@code FileUploadProperties.enableMd5Check}。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = Md5RequiredValidator.class)
public @interface Md5Required {
    String message() default "文件MD5不能为空";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
