package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link NoWildcard} 校验器，拒绝包含 {@code *} 的字符串。
 */
public class NoWildcardValidator implements ConstraintValidator<NoWildcard, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StrUtils.hasText(value)) {
            return true;
        }
        return !value.contains("*");
    }
}
