package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * {@link MaxUploadFileSize} 校验器，基于 {@link FileUploadProperties#getMaxFileSize()} 配置。
 */
@Component
public class MaxUploadFileSizeValidator implements ConstraintValidator<MaxUploadFileSize, Long> {

    private final FileUploadProperties fileUploadProperties;

    public MaxUploadFileSizeValidator(FileUploadProperties fileUploadProperties) {
        this.fileUploadProperties = fileUploadProperties;
    }

    @Override
    public boolean isValid(Long fileSize, ConstraintValidatorContext context) {
        if (fileSize == null) {
            return true;
        }
        Long maxFileSize = fileUploadProperties.getMaxFileSize();
        return maxFileSize == null || fileSize <= maxFileSize;
    }
}
