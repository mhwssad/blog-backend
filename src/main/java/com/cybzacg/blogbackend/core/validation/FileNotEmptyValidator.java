package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link FileNotEmpty} 校验器，支持单个文件和文件数组。
 */
public class FileNotEmptyValidator implements ConstraintValidator<FileNotEmpty, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        if (value instanceof MultipartFile file) {
            return !file.isEmpty();
        }
        if (value instanceof MultipartFile[] files) {
            if (files.length == 0) {
                return false;
            }
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
