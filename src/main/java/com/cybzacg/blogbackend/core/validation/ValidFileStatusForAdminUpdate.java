package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验后台文件状态更新值是否合法。
 * 禁止设置为 {@code DELETED(0)} 和 {@code PHYSICAL_DELETE_PENDING(2)}，
 * 这些状态由系统自动管理或通过专用删除接口触发。
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidFileStatusForAdminUpdateValidator.class)
public @interface ValidFileStatusForAdminUpdate {
    String message() default "文件状态值非法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
