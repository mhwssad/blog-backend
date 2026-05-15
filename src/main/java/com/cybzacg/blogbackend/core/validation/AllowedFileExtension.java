package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验文件扩展名是否在配置的白名单内。
 * 白名单为空时不限制；扩展名为空时默认允许。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AllowedFileExtensionValidator.class)
public @interface AllowedFileExtension {
    String message() default "不支持的文件类型";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
