package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.enums.ai.*;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
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

    static void validateToolSource(String sourceType) {
        ExceptionThrowerCore.throwBusinessIf(!AiToolSourceTypeEnum.contains(sourceType),
                ResultErrorCode.AI_TOOL_SOURCE_INVALID);
    }

    static void validateRiskLevel(String riskLevel) {
        ExceptionThrowerCore.throwBusinessIf(!AiToolRiskLevelEnum.contains(riskLevel),
                ResultErrorCode.AI_TOOL_RISK_INVALID);
    }

    static void validateTransportType(String transportType) {
        ExceptionThrowerCore.throwBusinessIf(!AiMcpTransportTypeEnum.contains(transportType),
                ResultErrorCode.AI_MCP_TRANSPORT_INVALID);
    }

    static void validateAuthorizationType(String authorizationType) {
        ExceptionThrowerCore.throwBusinessIf(!AiToolAuthorizationTypeEnum.contains(authorizationType),
                ResultErrorCode.ILLEGAL_ARGUMENT, "工具授权类型无效");
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

    static void validateJsonArrayOfToolScopes(String json) {
        if (!StrUtils.hasText(json)) {
            return;
        }
        List<String> scopes;
        try {
            scopes = JsonUtils.getObjectMapper().readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.ILLEGAL_ARGUMENT, "适用场景必须是字符串数组 JSON", e);
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(scopes == null || scopes.stream().anyMatch(scope -> !AiToolScopeEnum.contains(scope)),
                ResultErrorCode.ILLEGAL_ARGUMENT, "适用场景包含未知配置");
    }

    static void validateDataScope(String dataScope) {
        if (!StrUtils.hasText(dataScope)) {
            return;
        }
        ExceptionThrowerCore.throwBusinessIf(
                AiDataScopeEnum.fromCode(dataScope) == null && !isDataScopeName(dataScope),
                ResultErrorCode.ILLEGAL_ARGUMENT, "数据范围无效");
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
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (isSensitiveKey(key)) {
                    map.put(key, "******");
                } else {
                    map.put(key, maskNode(entry.getValue()));
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

    private static boolean isDataScopeName(String dataScope) {
        try {
            AiDataScopeEnum.valueOf(dataScope);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
