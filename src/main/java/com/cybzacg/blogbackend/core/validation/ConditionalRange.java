package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 类级别条件校验：当 {@code conditionField} 的值等于 {@code conditionValue} 时，
 * 校验 {@code field} 的数值在 [{@code min}, {@code max}] 范围内。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ConditionalRangeValidator.class)
public @interface ConditionalRange {
    String field();

    String conditionField();

    String conditionValue();

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    String message() default "值不在有效范围内";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
