package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验 JSON 字符串为合法的 {@code AiDataScopeEnum} 值数组。
 * null 或空串视为合法（由 {@code @NotBlank} 管理必填）。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidDataScopeArrayValidator.class)
public @interface ValidDataScopeArray {
    String message() default "数据范围配置包含无效值";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
