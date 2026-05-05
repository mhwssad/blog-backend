package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.domain.ai.AiToolAuthorization;
import com.cybzacg.blogbackend.domain.ai.AiToolCallLog;
import com.cybzacg.blogbackend.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.enums.ai.AiToolAuthorizationTypeEnum;
import com.cybzacg.blogbackend.enums.ai.AiToolSourceTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiToolExecutionContext;
import com.cybzacg.blogbackend.module.ai.repository.AiMcpServerConfigRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolAuthorizationRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolCallLogRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiToolDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.service.AiToolExecutionService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AI 工具统一执行服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolExecutionServiceImpl implements AiToolExecutionService {

    private final AiToolDefinitionRepository aiToolDefinitionRepository;
    private final AiToolAuthorizationRepository aiToolAuthorizationRepository;
    private final AiToolCallLogRepository aiToolCallLogRepository;
    private final AiMcpServerConfigRepository aiMcpServerConfigRepository;
    private final AiMcpClientFactory aiMcpClientFactory;

    @Override
    public void validateAuthorized(String toolCode, AiToolExecutionContext context) {
        AiToolDefinition tool = getEnabledTool(toolCode);
        validateAuthorization(tool, context);
    }

    @Override
    public AiToolExecuteVO execute(String toolCode, String arguments, AiToolExecutionContext context) {
        AiToolDefinition tool = getEnabledTool(toolCode);
        context.setToolId(tool.getId());
        context.setToolCode(tool.getToolCode());
        context.setToolName(tool.getToolName());
        validateAuthorization(tool, context);
        validateArguments(arguments, tool);

        long start = System.currentTimeMillis();
        AiToolCallLog callLog = buildCallLog(tool, arguments, context);
        try {
            String resultText = doExecute(tool, arguments);
            callLog.setSuccessStatus(1);
            callLog.setResponseSummary(AiToolSupport.summarize(resultText));
            callLog.setElapsedMs(System.currentTimeMillis() - start);
            aiToolCallLogRepository.save(callLog);
            return buildResult(true, resultText, null, callLog);
        } catch (Exception e) {
            log.warn("AI 工具调用失败: toolCode={}", tool.getToolCode(), e);
            callLog.setSuccessStatus(0);
            callLog.setErrorMessage(AiToolSupport.summarize(e.getMessage()));
            callLog.setElapsedMs(System.currentTimeMillis() - start);
            aiToolCallLogRepository.save(callLog);
            return buildResult(false, null, e.getMessage(), callLog);
        }
    }

    private AiToolDefinition getEnabledTool(String toolCode) {
        AiToolDefinition tool = ExceptionThrowerCore.requireNonNull(
                aiToolDefinitionRepository.findByToolCode(toolCode),
                ResultErrorCode.AI_TOOL_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(tool.getEnabled()),
                ResultErrorCode.AI_TOOL_DISABLED);
        return tool;
    }

    private void validateAuthorization(AiToolDefinition tool, AiToolExecutionContext context) {
        boolean authorized = aiToolAuthorizationRepository.listEnabledByToolId(tool.getId()).stream()
                .anyMatch(auth -> matchesAuthorization(auth, context));
        ExceptionThrowerCore.throwBusinessIf(!authorized, ResultErrorCode.AI_TOOL_UNAUTHORIZED);
    }

    private boolean matchesAuthorization(AiToolAuthorization authorization, AiToolExecutionContext context) {
        AiToolAuthorizationTypeEnum type = AiToolAuthorizationTypeEnum.fromCode(authorization.getAuthorizationType());
        if (type == AiToolAuthorizationTypeEnum.AGENT) {
            return context.getAgentId() != null
                    && Objects.equals(String.valueOf(context.getAgentId()), authorization.getAuthorizationKey());
        }
        if (type == AiToolAuthorizationTypeEnum.SCENE) {
            return StringUtils.hasText(context.getSceneType())
                    && context.getSceneType().equalsIgnoreCase(authorization.getAuthorizationKey());
        }
        if (type == AiToolAuthorizationTypeEnum.PERMISSION) {
            Set<String> authorities = context.getAuthorities();
            return authorities != null && authorities.contains(authorization.getAuthorizationKey());
        }
        if (type == AiToolAuthorizationTypeEnum.DATA_SCOPE) {
            return StringUtils.hasText(context.getDataScope())
                    && context.getDataScope().equalsIgnoreCase(authorization.getAuthorizationKey());
        }
        return false;
    }

    /**
     * v1 做基础 JSON Schema 校验：要求参数为 JSON 对象，且 required 字段存在。
     */
    private void validateArguments(String arguments, AiToolDefinition tool) {
        AiToolSupport.validateJsonObjectOrBlank(arguments, ResultErrorCode.ILLEGAL_ARGUMENT, "工具入参必须是 JSON 对象");
        if (!StringUtils.hasText(tool.getParametersSchema())) {
            return;
        }
        Map<String, Object> schema = AiToolSupport.parseJsonObject(tool.getParametersSchema(), "工具参数 Schema 无效");
        Object required = schema.get("required");
        if (!(required instanceof java.util.List<?> requiredList) || requiredList.isEmpty()) {
            return;
        }
        Map<String, Object> args = AiToolSupport.parseJsonObject(arguments, "工具入参必须是 JSON 对象");
        for (Object item : requiredList) {
            String field = String.valueOf(item);
            ExceptionThrowerCore.throwBusinessIf(!args.containsKey(field),
                    ResultErrorCode.ILLEGAL_ARGUMENT, "工具入参缺少必填字段: " + field);
        }
    }

    private String doExecute(AiToolDefinition tool, String arguments) throws Exception {
        AiToolSourceTypeEnum sourceType = AiToolSourceTypeEnum.fromCode(tool.getSourceType());
        if (sourceType == AiToolSourceTypeEnum.BUILTIN) {
            return ExceptionThrowerCore.throwBusiness(ResultErrorCode.AI_TOOL_EXECUTION_FAILED,
                    "内置工具尚未绑定执行器");
        }
        if (sourceType == AiToolSourceTypeEnum.MCP) {
            return executeMcpTool(tool, arguments);
        }
        return ExceptionThrowerCore.throwBusiness(ResultErrorCode.AI_TOOL_SOURCE_INVALID);
    }

    private String executeMcpTool(AiToolDefinition tool, String arguments) throws Exception {
        AiMcpServerConfig serverConfig = ExceptionThrowerCore.requireNonNull(
                aiMcpServerConfigRepository.getById(tool.getMcpServerId()),
                ResultErrorCode.AI_MCP_SERVER_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(serverConfig.getEnabled()),
                ResultErrorCode.AI_MCP_SERVER_DISABLED);
        try (McpClient client = aiMcpClientFactory.createClient(serverConfig)) {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name(tool.getMcpToolName())
                    .arguments(StringUtils.hasText(arguments) ? arguments : "{}")
                    .build();
            ToolExecutionResult result = client.executeTool(request);
            ExceptionThrowerCore.throwBusinessIf(result.isError(),
                    ResultErrorCode.AI_TOOL_EXECUTION_FAILED,
                    result.resultText());
            return result.resultText();
        }
    }

    private AiToolCallLog buildCallLog(AiToolDefinition tool, String arguments, AiToolExecutionContext context) {
        AiToolCallLog callLog = new AiToolCallLog();
        callLog.setUserId(context.getUserId());
        callLog.setAgentId(context.getAgentId());
        callLog.setSessionId(context.getSessionId());
        callLog.setTaskId(context.getTaskId());
        callLog.setToolId(tool.getId());
        callLog.setToolCode(tool.getToolCode());
        callLog.setToolName(tool.getToolName());
        callLog.setRequestSceneType(context.getSceneType());
        callLog.setRequestSummary(AiToolSupport.summarize(arguments));
        callLog.setSuccessStatus(0);
        return callLog;
    }

    private AiToolExecuteVO buildResult(boolean success, String resultText, String errorMessage, AiToolCallLog callLog) {
        AiToolExecuteVO vo = new AiToolExecuteVO();
        vo.setSuccess(success);
        vo.setResultText(success ? resultText : null);
        vo.setErrorMessage(errorMessage);
        vo.setElapsedMs(callLog.getElapsedMs());
        vo.setCallLogId(callLog.getId());
        return vo;
    }
}
