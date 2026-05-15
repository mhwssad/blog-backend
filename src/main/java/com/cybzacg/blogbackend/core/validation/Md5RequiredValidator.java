package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * {@link Md5Required} 校验器，仅在 {@code FileUploadProperties.enableMd5Check=true} 时强制非空。
 */
@Component
public class Md5RequiredValidator implements ConstraintValidator<Md5Required, String> {

    private final FileUploadProperties fileUploadProperties;

    public Md5RequiredValidator(FileUploadProperties fileUploadProperties) {
        this.fileUploadProperties = fileUploadProperties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!Boolean.TRUE.equals(fileUploadProperties.getEnableMd5Check())) {
            return true;
        }
        return StrUtils.hasText(value);
    }
}
