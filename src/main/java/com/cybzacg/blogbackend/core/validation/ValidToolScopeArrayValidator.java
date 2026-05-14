package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.enums.ai.AiToolScopeEnum;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * {@link ValidToolScopeArray} 校验器，解析 JSON 数组并逐项校验是否为合法的 {@code AiToolScopeEnum} 值。
 */
public class ValidToolScopeArrayValidator implements ConstraintValidator<ValidToolScopeArray, String> {

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
            if (!AiToolScopeEnum.contains(scope)) {
                return false;
            }
        }
        return true;
    }
}
