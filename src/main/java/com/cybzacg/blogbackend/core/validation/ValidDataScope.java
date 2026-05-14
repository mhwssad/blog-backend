package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验字符串是否为合法的 {@code AiDataScopeEnum} 编码或名称。
 * null 或空串视为合法（由 {@code @NotBlank} 管理必填）。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidDataScopeValidator.class)
public @interface ValidDataScope {
    String message() default "数据范围无效";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
