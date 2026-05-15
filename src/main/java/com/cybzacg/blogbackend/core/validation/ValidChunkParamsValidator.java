package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * {@link ValidChunkParams} 校验器，确保分片参数的逻辑一致性。
 */
public class ValidChunkParamsValidator implements ConstraintValidator<ValidChunkParams, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }
        Integer totalChunks = getFieldValue(obj, "totalChunks");
        Long chunkSize = getFieldValue(obj, "chunkSize");
        boolean hasExplicit = totalChunks != null || chunkSize != null;

        if (totalChunks != null && totalChunks <= 1) {
            disableDefaultAndBuildMessage(context, "分片总数必须大于1");
            return false;
        }
        if (chunkSize != null && chunkSize <= 0) {
            disableDefaultAndBuildMessage(context, "分片大小必须大于0");
            return false;
        }
        if (hasExplicit && (totalChunks == null || chunkSize == null)) {
            return false;
        }
        return true;
    }

    private void disableDefaultAndBuildMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
