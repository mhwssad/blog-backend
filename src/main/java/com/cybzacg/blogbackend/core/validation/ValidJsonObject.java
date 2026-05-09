package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验字符串字段是否为合法 JSON 对象。null 或空字符串视为合法。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidJsonObjectValidator.class)
public @interface ValidJsonObject {
    String message() default "必须是合法的 JSON 对象";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
