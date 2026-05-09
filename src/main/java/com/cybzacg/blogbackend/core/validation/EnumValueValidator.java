package com.cybzacg.blogbackend.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * {@link EnumValue} 校验器，通过反射调用枚举的 {@code getValue()} 获取有效值列表。
 * null 和空字符串视为合法，仅当值非空时才执行校验。
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private EnumValue annotation;
    private Method valueMethod;

    @Override
    public void initialize(EnumValue annotation) {
        this.annotation = annotation;
        try {
            this.valueMethod = annotation.enumClass().getMethod(annotation.method());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    annotation.enumClass().getSimpleName() + " 不存在方法 " + annotation.method() + "()", e);
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s && !StringUtils.hasText(s)) {
            return true;
        }

        Class<? extends Enum<?>> enumClass = annotation.enumClass();
        try {
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                Object enumValue = valueMethod.invoke(constant);
                if (match(value, enumValue)) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return false;
    }

    /**
     * 匹配输入值与枚举值，支持字符串忽略大小写比较。
     */
    private boolean match(Object input, Object enumValue) {
        if (input.equals(enumValue)) {
            return true;
        }
        if (input instanceof String s && enumValue instanceof String ev) {
            return s.equalsIgnoreCase(ev);
        }
        return false;
    }
}
