package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * {@link AtLeastOneNonNull} 校验器，通过反射检查指定字段中是否至少有一个非空。
 */
public class AtLeastOneNonNullValidator implements ConstraintValidator<AtLeastOneNonNull, Object> {

    private String[] fieldNames;

    @Override
    public void initialize(AtLeastOneNonNull annotation) {
        this.fieldNames = annotation.fields();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }
        for (String fieldName : fieldNames) {
            Object value = getFieldValue(obj, fieldName);
            if (isNonEmpty(value)) {
                return true;
            }
        }
        return false;
    }

    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private static boolean isNonEmpty(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return StrUtils.hasText(s);
        }
        if (value instanceof Collection<?> c) {
            return !c.isEmpty();
        }
        return true;
    }
}
