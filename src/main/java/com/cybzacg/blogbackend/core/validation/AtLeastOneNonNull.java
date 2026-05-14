package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 类级别校验：指定字段中至少有一个非空。
 * String 类型额外检查 {@code hasText}，Collection 类型检查非空集合。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AtLeastOneNonNullValidator.class)
public @interface AtLeastOneNonNull {
    String[] fields();

    String message() default "至少需要填写一个条件";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
