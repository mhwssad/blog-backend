package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验上传文件不为空。支持 {@link org.springframework.web.multipart.MultipartFile}
 * 和 {@code MultipartFile[]}。
 *
 * <p>单文件：不为 null 且 {@code isEmpty()} 返回 false。
 * <p>数组：不为 null、长度 > 0，且每个元素均不为空。
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = FileNotEmptyValidator.class)
public @interface FileNotEmpty {
    String message() default "上传文件不能为空";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
