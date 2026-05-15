package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * {@link AtLeastOneTrue} 校验器，通过反射检查指定 Boolean 字段中是否至少有一个为 {@code true}。
 */
public class AtLeastOneTrueValidator implements ConstraintValidator<AtLeastOneTrue, Object> {

    private String[] fieldNames;

    @Override
    public void initialize(AtLeastOneTrue annotation) {
        this.fieldNames = annotation.fields();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }
        for (String fieldName : fieldNames) {
            Object value = getFieldValue(obj, fieldName);
            if (Boolean.TRUE.equals(value)) {
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
}
