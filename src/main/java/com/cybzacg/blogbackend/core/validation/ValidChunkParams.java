package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 类级别跨字段校验：分片参数一致性。
 * 若提供了任一分片参数（totalChunks / chunkSize），则要求：
 * <ul>
 *   <li>totalChunks > 1</li>
 *   <li>chunkSize > 0</li>
 *   <li>totalChunks 和 chunkSize 必须同时提供</li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ValidChunkParamsValidator.class)
public @interface ValidChunkParams {
    String message() default "分片参数不完整";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
