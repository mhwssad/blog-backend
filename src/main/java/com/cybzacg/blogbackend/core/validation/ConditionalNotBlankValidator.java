package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * {@link ConditionalNotBlank} 校验器。
 */
public class ConditionalNotBlankValidator implements ConstraintValidator<ConditionalNotBlank, Object> {

    private String fieldName;
    private String dependsOnName;
    private String[] triggerValues;

    @Override
    public void initialize(ConditionalNotBlank annotation) {
        this.fieldName = annotation.field();
        this.dependsOnName = annotation.dependsOn();
        this.triggerValues = annotation.values();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }
        Object dependsOnValue = getFieldValue(obj, dependsOnName);
        if (!isTriggered(dependsOnValue)) {
            return true;
        }
        Object fieldValue = getFieldValue(obj, fieldName);
        if (fieldValue instanceof String s) {
            return StrUtils.hasText(s);
        }
        return fieldValue != null;
    }

    private boolean isTriggered(Object value) {
        if (value == null) {
            return false;
        }
        String strValue = String.valueOf(value);
        return Arrays.asList(triggerValues).contains(strValue);
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
