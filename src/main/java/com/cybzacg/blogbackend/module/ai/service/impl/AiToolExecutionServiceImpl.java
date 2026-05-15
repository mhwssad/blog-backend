package com.cybzacg.blogbackend.module.ai.service.impl;

import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisKeyUtils;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.domain.ai.AiMcpServerConfig;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolAuthorization;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolCallLog;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolDefinition;
import com.cybzacg.blogbackend.dto.repository.ai.AiMcpServerConfigRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiToolAuthorizationRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiToolCallLogRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiToolDefinitionRepository;
import com.cybzacg.blogbackend.enums.ai.AiToolAuthorizationTypeEnum;
import com.cybzacg.blogbackend.enums.ai.AiToolSourceTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;
import com.cybzacg.blogbackend.module.ai.model.internal.AiToolExecutionContext;
import com.cybzacg.blogbackend.module.ai.service.AiToolExecutionService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final RedisOperator redisOperator;

    /**
     * 校验指定工具在当前上下文中是否已授权。
     *
     * @param toolCode 工具编码
     * @param context  工具执行上下文，包含用户、场景、权限等信息
     * @throws com.cybzacg.blogbackend.exception.BusinessException 工具不存在、已停用或未授权时抛出
     */
    @Override
    public void validateAuthorized(String toolCode, AiToolExecutionContext context) {
        AiToolDefinition tool = getEnabledTool(toolCode);
        validateAuthorization(tool, context);
    }

    /**
     * 执行指定工具：校验授权、限流、参数后调用实际执行器，记录调用日志。
     *
     * @param toolCode  工具编码
     * @param arguments 工具入参 JSON 字符串
     * @param context   工具执行上下文
     * @return 执行结果 VO，包含输出文本、耗时、调用日志 ID 等
     */
    @Override
    public AiToolExecuteVO execute(String toolCode, String arguments, AiToolExecutionContext context) {
        AiToolDefinition tool = getEnabledTool(toolCode);
        context.setToolId(tool.getId());
        context.setToolCode(tool.getToolCode());
        context.setToolName(tool.getToolName());
        validateAuthorization(tool, context);
        enforceRateLimit(tool, context);
        validateArguments(arguments, tool);

        long start = System.currentTimeMillis();
        AiToolCallLog callLog = buildCallLog(tool, arguments, context);
        try {
            String resultText = doExecute(tool, arguments);
            callLog.setSuccessStatus(1);
            callLog.setResponseSummary(AiToolSupport.summarize(resultText));
            callLog.setElapsedMs(System.currentTimeMillis() - start);
            aiToolCallLogRepository.save(callLog);
            log.info("AI 工具调用成功: toolCode={}, elapsedMs={}", toolCode, callLog.getElapsedMs());
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

    /**
     * 查找并校验工具是否启用。
     *
     * @param toolCode 工具编码
     * @return 启用状态的工具定义
     */
    private AiToolDefinition getEnabledTool(String toolCode) {
        AiToolDefinition tool = ExceptionThrowerCore.requireNonNull(
                aiToolDefinitionRepository.findByToolCode(toolCode),
                ResultErrorCode.AI_TOOL_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(tool.getEnabled()),
                ResultErrorCode.AI_TOOL_DISABLED);
        return tool;
    }

    /**
     * 校验工具在当前上下文中是否有任一授权规则匹配。
     * 支持 AGENT、SCENE、PERMISSION、DATA_SCOPE 四种授权类型。
     *
     * @param tool    工具定义
     * @param context 执行上下文
     */
    private void validateAuthorization(AiToolDefinition tool, AiToolExecutionContext context) {
        boolean authorized = aiToolAuthorizationRepository.listEnabledByToolId(tool.getId()).stream()
                .anyMatch(auth -> matchesAuthorization(auth, context));
        ExceptionThrowerCore.throwBusinessIf(!authorized, ResultErrorCode.AI_TOOL_UNAUTHORIZED);
    }

    /**
     * 判断单条授权规则是否与当前上下文匹配。
     *
     * @param authorization 授权规则
     * @param context       执行上下文
     * @return 是否匹配
     */
    private boolean matchesAuthorization(AiToolAuthorization authorization, AiToolExecutionContext context) {
        AiToolAuthorizationTypeEnum type = AiToolAuthorizationTypeEnum.fromCode(authorization.getAuthorizationType());
        if (type == AiToolAuthorizationTypeEnum.AGENT) {
            return context.getAgentId() != null
                    && Objects.equals(String.valueOf(context.getAgentId()), authorization.getAuthorizationKey());
        }
        if (type == AiToolAuthorizationTypeEnum.SCENE) {
            return StrUtils.hasText(context.getSceneType())
                    && context.getSceneType().equalsIgnoreCase(authorization.getAuthorizationKey());
        }
        if (type == AiToolAuthorizationTypeEnum.PERMISSION) {
            Set<String> authorities = context.getAuthorities();
            return authorities != null && authorities.contains(authorization.getAuthorizationKey());
        }
        if (type == AiToolAuthorizationTypeEnum.DATA_SCOPE) {
            return StrUtils.hasText(context.getDataScope())
                    && context.getDataScope().equalsIgnoreCase(authorization.getAuthorizationKey());
        }
        return false;
    }

    /**
     * v1 做基础 JSON Schema 校验：要求参数为 JSON 对象，且 required 字段存在。
     */
    private void validateArguments(String arguments, AiToolDefinition tool) {
        AiToolSupport.validateJsonObjectOrBlank(arguments, ResultErrorCode.ILLEGAL_ARGUMENT, "工具入参必须是 JSON 对象");
        if (!StrUtils.hasText(tool.getParametersSchema())) {
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

    /**
     * 基于 Redis 的简单限流：每用户每工具每分钟最多 30 次调用。
     *
     * @param tool    工具定义
     * @param context 执行上下文，用于提取限流主体
     */
    private void enforceRateLimit(AiToolDefinition tool, AiToolExecutionContext context) {
        String subject = context.getUserId() != null ? String.valueOf(context.getUserId()) : "anonymous";
        String key = RedisKeyUtils.build(RedisConstants.AI_TOOL_EXECUTE_RATE_LIMIT_PREFIX,
                tool.getToolCode(), subject);
        long count = redisOperator.increment(key);
        if (count == 1) {
            redisOperator.expire(key, Duration.ofMinutes(1));
        }
        ExceptionThrowerCore.throwBusinessIf(count > 30,
                ResultErrorCode.REQUEST_RATE_LIMITED,
                "工具调用过于频繁，请稍后再试");
    }

    /**
     * 根据工具来源类型分发执行：MCP 类型调用远程服务，内置类型暂未实现。
     *
     * @param tool      工具定义
     * @param arguments 入参 JSON
     * @return 工具执行结果文本
     * @throws Exception MCP 调用可能抛出连接异常
     */
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

    /**
     * 通过 MCP 客户端执行远程工具调用，使用 try-with-resources 确保客户端关闭。
     *
     * @param tool      工具定义，包含 MCP 服务和工具名称映射
     * @param arguments 入参 JSON
     * @return 工具返回结果文本
     * @throws Exception 连接或执行异常
     */
    private String executeMcpTool(AiToolDefinition tool, String arguments) throws Exception {
        AiMcpServerConfig serverConfig = ExceptionThrowerCore.requireNonNull(
                aiMcpServerConfigRepository.getById(tool.getMcpServerId()),
                ResultErrorCode.AI_MCP_SERVER_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(!Integer.valueOf(1).equals(serverConfig.getEnabled()),
                ResultErrorCode.AI_MCP_SERVER_DISABLED);
        try (McpClient client = aiMcpClientFactory.createClient(serverConfig)) {
            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name(tool.getMcpToolName())
                    .arguments(StrUtils.hasText(arguments) ? arguments : "{}")
                    .build();
            ToolExecutionResult result = client.executeTool(request);
            ExceptionThrowerCore.throwBusinessIf(result.isError(),
                    ResultErrorCode.AI_TOOL_EXECUTION_FAILED,
                    result.resultText());
            return result.resultText();
        }
    }

    /**
     * 构建工具调用日志实体（初始状态为未成功）。
     *
     * @param tool      工具定义
     * @param arguments 入参 JSON
     * @param context   执行上下文
     * @return 调用日志实体
     */
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

    /**
     * 构建工具执行结果 VO。
     *
     * @param success     是否成功
     * @param resultText  成功时的结果文本
     * @param errorMessage 失败时的错误信息
     * @param callLog     调用日志实体（用于获取耗时和 ID）
     * @return 执行结果 VO
     */
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
