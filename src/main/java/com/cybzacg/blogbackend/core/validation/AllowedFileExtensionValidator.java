package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.utils.FileUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * {@link AllowedFileExtension} 校验器，基于 {@link FileUploadProperties#getAllowedExtensions()} 白名单。
 */
@Component
public class AllowedFileExtensionValidator implements ConstraintValidator<AllowedFileExtension, String> {

    private final FileUploadProperties fileUploadProperties;

    public AllowedFileExtensionValidator(FileUploadProperties fileUploadProperties) {
        this.fileUploadProperties = fileUploadProperties;
    }

    @Override
    public boolean isValid(String fileName, ConstraintValidatorContext context) {
        if (!StrUtils.hasText(fileName)) {
            return true;
        }
        String ext = FileUtils.getExtension(fileName);
        if (!StrUtils.hasText(ext)) {
            return true;
        }
        List<String> allowed = fileUploadProperties.getAllowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        String normalized = ext.toLowerCase(Locale.ROOT);
        for (String item : allowed) {
            if (StrUtils.hasText(item) && normalized.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }
}
