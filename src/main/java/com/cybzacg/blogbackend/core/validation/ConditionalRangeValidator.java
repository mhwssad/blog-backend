package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * {@link ConditionalRange} 校验器。
 */
public class ConditionalRangeValidator implements ConstraintValidator<ConditionalRange, Object> {

    private String fieldName;
    private String conditionFieldName;
    private String conditionValue;
    private long min;
    private long max;

    @Override
    public void initialize(ConditionalRange annotation) {
        this.fieldName = annotation.field();
        this.conditionFieldName = annotation.conditionField();
        this.conditionValue = annotation.conditionValue();
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }
        Object condValue = getFieldValue(obj, conditionFieldName);
        if (!conditionValue.equals(String.valueOf(condValue))) {
            return true;
        }
        Object fieldValue = getFieldValue(obj, fieldName);
        if (fieldValue instanceof Number number) {
            long longValue = number.longValue();
            return longValue >= min && longValue <= max;
        }
        return true;
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
