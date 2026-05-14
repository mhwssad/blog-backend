package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.enums.ai.AiDataScopeEnum;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * {@link ValidDataScopeArray} 校验器，解析 JSON 数组并逐项校验是否为合法的 {@code AiDataScopeEnum} 值。
 */
public class ValidDataScopeArrayValidator implements ConstraintValidator<ValidDataScopeArray, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StrUtils.hasText(value)) {
            return true;
        }
        List<String> scopes;
        try {
            scopes = JsonUtils.getObjectMapper().readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            return false;
        }
        if (scopes == null) {
            return false;
        }
        for (String scope : scopes) {
            if (!isValidScope(scope)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidScope(String scope) {
        if (!StrUtils.hasText(scope)) {
            return false;
        }
        if (AiDataScopeEnum.fromCode(scope) != null) {
            return true;
        }
        try {
            AiDataScopeEnum.valueOf(scope);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
