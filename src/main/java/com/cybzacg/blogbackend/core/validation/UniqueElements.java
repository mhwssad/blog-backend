package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 集合字段校验：元素不能为 null 且不能重复。集合本身允许为 null 或空。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UniqueElementsValidator.class)
public @interface UniqueElements {
    String message() default "列表元素不能为空且不能重复";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
