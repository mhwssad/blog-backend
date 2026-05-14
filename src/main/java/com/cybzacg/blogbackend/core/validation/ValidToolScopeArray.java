package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验 JSON 字符串为合法的 {@code AiToolScopeEnum} 值数组。
 * null 或空串视为合法（由 {@code @NotBlank} 管理必填）。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidToolScopeArrayValidator.class)
public @interface ValidToolScopeArray {
    String message() default "适用场景包含无效配置";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
