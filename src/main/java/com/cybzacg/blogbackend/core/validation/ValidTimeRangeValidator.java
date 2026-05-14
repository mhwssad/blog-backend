package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * {@link ValidTimeRange} 校验器。
 */
public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, Object> {

    private String startFieldName;
    private String endFieldName;
    private String conditionFieldName;
    private String conditionValue;
    private long maxDays;

    @Override
    public void initialize(ValidTimeRange annotation) {
        this.startFieldName = annotation.startField();
        this.endFieldName = annotation.endField();
        this.conditionFieldName = annotation.conditionField();
        this.conditionValue = annotation.conditionValue();
        this.maxDays = annotation.maxDays();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }
        if (!conditionFieldName.isEmpty()) {
            Object condValue = getFieldValue(obj, conditionFieldName);
            if (condValue == null || !conditionValue.equals(String.valueOf(condValue))) {
                return true;
            }
        }
        LocalDateTime startTime = toLocalDateTime(getFieldValue(obj, startFieldName));
        LocalDateTime endTime = toLocalDateTime(getFieldValue(obj, endFieldName));

        context.disableDefaultConstraintViolation();
        if (startTime == null || endTime == null) {
            context.buildConstraintViolationWithTemplate("自定义时间范围必须同时传入开始时间和结束时间")
                    .addConstraintViolation();
            return false;
        }
        if (!startTime.isBefore(endTime)) {
            context.buildConstraintViolationWithTemplate("开始时间必须早于结束时间")
                    .addConstraintViolation();
            return false;
        }
        if (maxDays > 0 && Duration.between(startTime, endTime).toDays() > maxDays) {
            context.buildConstraintViolationWithTemplate("自定义时间范围不能超过" + maxDays + "天")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        return null;
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
