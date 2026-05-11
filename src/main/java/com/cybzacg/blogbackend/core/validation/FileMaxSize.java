package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 校验上传文件大小不超过指定上限。支持 {@link org.springframework.web.multipart.MultipartFile}
 * 和 {@code MultipartFile[]}。
 *
 * <p>单文件：文件大小 ≤ maxSize。
 * <p>数组：每个文件大小均 ≤ maxSize。
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = FileMaxSizeValidator.class)
public @interface FileMaxSize {
    /**
     * 最大文件大小，单位字节。
     */
    long maxSize();

    String message() default "文件大小超出限制";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
