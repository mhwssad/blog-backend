package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 类级别校验：当条件字段匹配指定值时，校验起止时间的完整性与合法性。
 *
 * <p>支持非空、先后顺序以及最大天数跨度的检查。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidTimeRangeValidator.class)
public @interface ValidTimeRange {
    String startField() default "startTime";

    String endField() default "endTime";

    String conditionField() default "";

    String conditionValue() default "";

    long maxDays() default -1;

    String message() default "时间范围无效";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
