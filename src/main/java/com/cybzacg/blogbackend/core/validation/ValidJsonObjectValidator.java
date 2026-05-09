package com.cybzacg.blogbackend.core.validation;

import com.cybzacg.blogbackend.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * {@link ValidJsonObject} 校验器，非空字符串必须是可解析的 JSON 对象。
 */
public class ValidJsonObjectValidator implements ConstraintValidator<ValidJsonObject, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(value);
            return node.isObject();
        } catch (Exception e) {
            return false;
        }
    }
}
