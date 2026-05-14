package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 类级别条件校验：当 {@code dependsOn} 字段值匹配 {@code values} 中任一值时，
 * 要求 {@code field} 指定的集合字段非空。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ConditionalNotEmpty.List.class)
@Constraint(validatedBy = ConditionalNotEmptyValidator.class)
public @interface ConditionalNotEmpty {
    String field();

    String dependsOn();

    String[] values();

    String message() default "当前条件下该字段不能为空";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ConditionalNotEmpty[] value();
    }
}
