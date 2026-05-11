package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验字段值是否在指定枚举的 {@code getValue()} 返回值范围内。
 * null 或空字符串视为合法（用于可选筛选字段）。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {
    Class<? extends Enum<?>> enumClass();

    /** 枚举取值方法名，默认 {@code "getValue"}。 */
    String method() default "getValue";

    String message() default "值不在有效范围内";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
