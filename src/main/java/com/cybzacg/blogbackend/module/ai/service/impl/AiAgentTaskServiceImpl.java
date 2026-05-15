package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentTaskLog;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentTaskLogRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentTaskRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.enums.ai.AiAgentTaskStatusEnum;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.internal.AiToolExecutionContext;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskVO;
import com.cybzacg.blogbackend.module.ai.service.*;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI Agent 任务用户侧服务实现。
 *
 * <p>负责用户发起任务、查询任务、取消任务和同步执行 agent 任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentTaskServiceImpl implements AiAgentTaskService {

    private final AiAgentTaskRepository aiAgentTaskRepository;
    private final AiAgentDefinitionRepository aiAgentDefinitionRepository;
    private final AiAgentTaskLogRepository aiAgentTaskLogRepository;
    private final AiChannelConfigRepository aiChannelConfigRepository;
    private final AiModelClient aiModelClient;
    private final AiQuotaService aiQuotaService;
    private final AiToolExecutionService aiToolExecutionService;
    private final AiUsageLogService aiUsageLogService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final AiModelConvert aiModelConvert;

    /**
     * 用户发起 Agent 任务。
     *
     * <p>校验 Agent 定义和渠道配置后，创建任务记录并同步执行。
     * 执行完成后根据结果发送通知。
     *
     * @param userId  当前用户 ID
     * @param request 任务创建请求，包含 agentId 和输入内容
     * @return 任务 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException Agent 不存在、已禁用或额度不足时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgentTaskVO createTask(Long userId, AiAgentTaskCreateRequest request) {
        AiAgentDefinition definition = validateAndGetDefinition(request.getAgentId());
        AiChannelConfig channelConfig = getChannelConfig(definition);

        aiQuotaService.checkQuota(userId, channelConfig);
        validateAgentToolWhitelist(definition, userId);

        AiAgentTask task = new AiAgentTask();
        task.setUserId(userId);
        task.setAgentId(definition.getId());
        task.setStatus(AiAgentTaskStatusEnum.PENDING.getValue());
        task.setInputContent(request.getInputContent());
        task.setTokenCount(0);
        aiAgentTaskRepository.save(task);

        log.info("创建 Agent 任务: taskId={}, userId={}, agentId={}", task.getId(), userId, request.getAgentId());
        executeTask(task, definition, channelConfig);

        AiAgentTaskVO vo = aiModelConvert.toAgentTaskVO(task);
        vo.setAgentName(definition.getName());
        return vo;
    }

    /**
     * 分页查询当前用户的 Agent 任务列表。
     *
     * @param userId 当前用户 ID
     * @param query  分页查询参数，支持按状态过滤
     * @return 分页结果
     */
    @Override
    public PageResult<AiAgentTaskVO> pageMyTasks(Long userId, AiAgentTaskPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Page<AiAgentTask> page = new Page<>(current, size);
        Page<AiAgentTask> result = aiAgentTaskRepository.pageByUserIdAndStatus(page, userId, query.getStatus());

        List<AiAgentTaskVO> voList = result.getRecords().stream().map(task -> {
            AiAgentTaskVO vo = aiModelConvert.toAgentTaskVO(task);
            AiAgentDefinition def = aiAgentDefinitionRepository.getById(task.getAgentId());
            if (def != null) {
                vo.setAgentName(def.getName());
            }
            return vo;
        }).toList();

        return PageResult.of(result, voList);
    }

    /**
     * 获取指定任务的详情（仅任务所有者可查看）。
     *
     * @param userId  当前用户 ID
     * @param taskId  任务 ID
     * @return 任务 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 任务不存在或不属于当前用户时抛出
     */
    @Override
    public AiAgentTaskVO getTask(Long userId, Long taskId) {
        AiAgentTask task = getAndValidateOwner(userId, taskId);
        AiAgentTaskVO vo = aiModelConvert.toAgentTaskVO(task);
        AiAgentDefinition def = aiAgentDefinitionRepository.getById(task.getAgentId());
        if (def != null) {
            vo.setAgentName(def.getName());
        }
        return vo;
    }

    /**
     * 取消指定任务（仅 PENDING 状态可取消）。
     *
     * @param userId  当前用户 ID
     * @param taskId  任务 ID
     * @throws com.cybzacg.blogbackend.exception.BusinessException 任务不存在、不属于当前用户或状态不允许取消时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long userId, Long taskId) {
        AiAgentTask task = getAndValidateOwner(userId, taskId);
        ExceptionThrowerCore.throwBusinessIf(
                !AiAgentTaskStatusEnum.PENDING.getValue().equals(task.getStatus()),
                ResultErrorCode.AI_AGENT_TASK_NOT_COMPLETABLE);

        task.setStatus(AiAgentTaskStatusEnum.CANCELLED.getValue());
        task.setCompletedAt(LocalDateTime.now());
        aiAgentTaskRepository.updateById(task);
        log.info("Agent 任务已取消: taskId={}, userId={}", taskId, userId);
    }

    /**
     * 同步执行 Agent 任务：调用模型、记录日志、扣减额度并推送通知。
     *
     * <p>无论成功或失败均会更新任务状态和完成时间，异常不会向上层传播。
     */
    private void executeTask(AiAgentTask task, AiAgentDefinition definition, AiChannelConfig channelConfig) {
        try {
            task.setStatus(AiAgentTaskStatusEnum.RUNNING.getValue());
            task.setStartedAt(LocalDateTime.now());
            aiAgentTaskRepository.updateById(task);

            AiModelCallResult result = aiModelClient.chat(
                    channelConfig, definition.getSystemPrompt(),
                    Collections.emptyList(), task.getInputContent());

            if (result.isSuccess()) {
                task.setStatus(AiAgentTaskStatusEnum.COMPLETED.getValue());
                task.setOutputContent(result.getContent());
                task.setTokenCount(result.getTotalTokens());
            } else {
                task.setStatus(AiAgentTaskStatusEnum.FAILED.getValue());
                task.setErrorMessage(result.getErrorMessage());
            }
            task.setCompletedAt(LocalDateTime.now());
            aiAgentTaskRepository.updateById(task);

            // 记录对话日志（用户输入 + 模型回复）
            recordLogs(task.getId(), task.getInputContent(), result);

            if (result.isSuccess()) {
                // 成功时扣减额度并记录使用量
                aiQuotaService.recordUsage(task.getUserId(), channelConfig.getId());
                aiUsageLogService.logUsage(task.getUserId(), channelConfig.getId(),
                        null, "agent", result.getRequestTokens(),
                        result.getResponseTokens(), result.getTotalTokens(), 1, null);

                notificationDeliveryService.deliverAfterCommit(
                        task.getUserId(),
                        NotificationTypeEnum.AI_TASK_DONE,
                        "Agent 任务完成",
                        definition.getName() + " 任务已完成",
                        null,
                        "ai_agent_task", task.getId(),
                        "/ai/agents/tasks/" + task.getId());
            } else {
                // 失败时仅记录使用量，不扣减额度
                aiUsageLogService.logUsage(task.getUserId(), channelConfig.getId(),
                        null, "agent", result.getRequestTokens(),
                        result.getResponseTokens(), result.getTotalTokens(), 0, "agent_call_failed");

                notificationDeliveryService.deliverAfterCommit(
                        task.getUserId(),
                        NotificationTypeEnum.AI_TASK_DONE,
                        "Agent 任务失败",
                        definition.getName() + " 任务执行失败",
                        null,
                        "ai_agent_task", task.getId(),
                        "/ai/agents/tasks/" + task.getId());
            }
        } catch (Exception e) {
            log.error("Agent 任务执行异常: taskId={}", task.getId(), e);
            task.setStatus(AiAgentTaskStatusEnum.FAILED.getValue());
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            aiAgentTaskRepository.updateById(task);

            notificationDeliveryService.deliverAfterCommit(
                    task.getUserId(),
                    NotificationTypeEnum.AI_TASK_DONE,
                    "Agent 任务失败",
                    "任务执行异常",
                    null,
                    "ai_agent_task", task.getId(),
                    "/ai/agents/tasks/" + task.getId());
        }
    }

    /**
     * 将用户输入和模型回复记录为任务日志（一轮对话）。
     */
    private void recordLogs(Long taskId, String inputContent, AiModelCallResult result) {
        AiAgentTaskLog userLog = new AiAgentTaskLog();
        userLog.setTaskId(taskId);
        userLog.setTurnIndex(0);
        userLog.setRoleType("user");
        userLog.setContent(inputContent);
        userLog.setTokenCount(0);
        aiAgentTaskLogRepository.save(userLog);

        AiAgentTaskLog assistantLog = new AiAgentTaskLog();
        assistantLog.setTaskId(taskId);
        assistantLog.setTurnIndex(1);
        assistantLog.setRoleType("assistant");
        assistantLog.setContent(result.getContent());
        assistantLog.setTokenCount(result.getTotalTokens());
        aiAgentTaskLogRepository.save(assistantLog);
    }

    /**
     * 校验 Agent 定义是否存在且已启用。
     *
     * @param agentId Agent 定义 ID
     * @return Agent 定义实体
     * @throws com.cybzacg.blogbackend.exception.BusinessException 不存在或已禁用时抛出
     */
    private AiAgentDefinition validateAndGetDefinition(Long agentId) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(agentId),
                ResultErrorCode.AI_AGENT_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(
                definition.getEnabled() != 1,
                ResultErrorCode.AI_AGENT_DISABLED);
        return definition;
    }

    /**
     * 根据 Agent 定义获取关联的渠道配置。
     */
    private AiChannelConfig getChannelConfig(AiAgentDefinition definition) {
        return ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(definition.getChannelConfigId()),
                ResultErrorCode.AI_CHANNEL_NOT_FOUND);
    }

    /**
     * Agent 执行前验证扩展配置中的工具白名单，v1 不允许模型调用未显式配置或未授权工具。
     */
    private void validateAgentToolWhitelist(AiAgentDefinition definition, Long userId) {
        List<String> allowedToolCodes = parseAllowedToolCodes(definition.getExtraConfigJson());
        if (allowedToolCodes.isEmpty()) {
            return;
        }
        AiToolExecutionContext context = AiToolExecutionContext.builder()
                .userId(userId)
                .agentId(definition.getId())
                .sceneType("agent")
                .dataScope(definition.getDataScopeJson())
                .authorities(SecurityUtils.getAuthoritySet())
                .build();
        allowedToolCodes.forEach(toolCode -> aiToolExecutionService.validateAuthorized(toolCode, context));
    }

    /**
     * 从 Agent 扩展配置 JSON 中解析允许调用的工具编码列表。
     *
     * @param extraConfigJson JSON 字符串，预期包含 "allowedToolCodes" 数组
     * @return 工具编码列表，解析失败或未配置时返回空列表
     * @throws com.cybzacg.blogbackend.exception.BusinessException JSON 格式非法时抛出
     */
    private List<String> parseAllowedToolCodes(String extraConfigJson) {
        if (extraConfigJson == null || extraConfigJson.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> config = JsonUtils.getObjectMapper().readValue(extraConfigJson, new TypeReference<>() {
            });
            Object allowed = config.get("allowedToolCodes");
            if (allowed instanceof List<?> allowedList) {
                return allowedList.stream().map(String::valueOf).filter(item -> !item.isBlank()).toList();
            }
            return List.of();
        } catch (JsonProcessingException e) {
            ExceptionThrowerCore.throwBusiness(ResultErrorCode.ILLEGAL_ARGUMENT, "Agent 扩展配置必须是 JSON 对象", e);
            return List.of();
        }
    }

    /**
     * 获取任务并验证是否属于指定用户。
     *
     * @param userId  当前用户 ID
     * @param taskId  任务 ID
     * @return 任务实体
     * @throws com.cybzacg.blogbackend.exception.BusinessException 任务不存在或不属于当前用户时抛出
     */
    private AiAgentTask getAndValidateOwner(Long userId, Long taskId) {
        AiAgentTask task = ExceptionThrowerCore.requireNonNull(
                aiAgentTaskRepository.getById(taskId),
                ResultErrorCode.AI_AGENT_TASK_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(
                !task.getUserId().equals(userId),
                ResultErrorCode.AI_AGENT_TASK_NOT_OWNER);
        return task;
    }
}
