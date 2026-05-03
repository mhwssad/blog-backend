package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link FileMaxSize} 校验器，支持单个文件和文件数组。
 */
public class FileMaxSizeValidator implements ConstraintValidator<FileMaxSize, Object> {

    private long maxSize;

    @Override
    public void initialize(FileMaxSize annotation) {
        this.maxSize = annotation.maxSize();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value instanceof MultipartFile file) {
            return file.getSize() <= maxSize;
        }
        if (value instanceof MultipartFile[] files) {
            for (MultipartFile file : files) {
                if (file != null && file.getSize() > maxSize) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}
