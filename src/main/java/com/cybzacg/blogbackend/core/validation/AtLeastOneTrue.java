package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 类级别校验：指定 Boolean 字段中至少有一个为 {@code true}。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AtLeastOneTrueValidator.class)
public @interface AtLeastOneTrue {
    String[] fields();

    String message() default "至少需要选择一个条件";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
