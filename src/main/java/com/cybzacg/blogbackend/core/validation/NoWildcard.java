package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验字符串字段不包含通配符 {@code *}。
 * null 或空串视为合法（由 {@code @NotBlank} 管理必填）。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NoWildcardValidator.class)
public @interface NoWildcard {
    String message() default "不允许包含通配符 *";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
