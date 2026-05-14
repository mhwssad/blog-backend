package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 工具与 MCP 公共校验和脱敏工具。
 */
final class AiToolSupport {
    private static final int SUMMARY_MAX_LENGTH = 1000;

    private AiToolSupport() {
    }

    static void validateEnabled(Integer enabled) {
        ExceptionThrowerCore.throwBusinessIf(enabled == null || (enabled != 0 && enabled != 1),
                ResultErrorCode.AI_TOOL_STATUS_INVALID);
    }

    static void validateJsonObjectOrBlank(String json, ResultErrorCode errorCode, String message) {
        if (!StrUtils.hasText(json)) {
            return;
        }
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(json);
            ExceptionThrowerCore.throwBusinessIf(!node.isObject(), errorCode, message);
        } catch (JsonProcessingException e) {
            ExceptionThrowerCore.throwBusiness(errorCode, message, e);
        }
    }

    static Map<String, Object> parseJsonObject(String json, String message) {
        validateJsonObjectOrBlank(json, ResultErrorCode.ILLEGAL_ARGUMENT, message);
        if (!StrUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return JsonUtils.getObjectMapper().readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return ExceptionThrowerCore.throwBusiness(ResultErrorCode.ILLEGAL_ARGUMENT, message, e);
        }
    }

    static String maskJson(String json) {
        if (!StrUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(json);
            return JsonUtils.toJson(maskNode(node));
        } catch (Exception e) {
            return "******";
        }
    }

    static String summarize(String value) {
        if (!StrUtils.hasText(value)) {
            return value;
        }
        String masked = value
                .replaceAll("(?i)(api[_-]?key|token|secret|password)\"\\s*:\\s*\"[^\"]*\"", "$1\":\"******\"")
                .replaceAll("(?i)(api[_-]?key|token|secret|password)=([^,&\\s]+)", "$1=******");
        if (masked.length() <= SUMMARY_MAX_LENGTH) {
            return masked;
        }
        return masked.substring(0, SUMMARY_MAX_LENGTH);
    }

    private static Object maskNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            com.fasterxml.jackson.databind.node.ObjectNode objNode = (com.fasterxml.jackson.databind.node.ObjectNode) node;
            objNode.fieldNames().forEachRemaining(key -> {
                if (isSensitiveKey(key)) {
                    map.put(key, "******");
                } else {
                    map.put(key, maskNode(objNode.get(key)));
                }
            });
            return map;
        }
        if (node.isArray()) {
            java.util.ArrayList<Object> list = new java.util.ArrayList<>();
            node.forEach(child -> list.add(maskNode(child)));
            return list;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return lower.contains("key") || lower.contains("token")
                || lower.contains("secret") || lower.contains("password");
    }
}
