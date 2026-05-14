package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.enums.ai.AiDataScopeEnum;
import com.cybzacg.blogbackend.utils.StrUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link ValidDataScope} 校验器，校验字符串是否为合法的 {@code AiDataScopeEnum} 编码或名称。
 */
public class ValidDataScopeValidator implements ConstraintValidator<ValidDataScope, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StrUtils.hasText(value)) {
            return true;
        }
        if (AiDataScopeEnum.fromCode(value) != null) {
            return true;
        }
        try {
            AiDataScopeEnum.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
