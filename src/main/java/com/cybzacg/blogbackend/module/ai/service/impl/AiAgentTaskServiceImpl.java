package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.domain.ai.AiAgentTaskLog;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.enums.ai.AiAgentTaskStatusEnum;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskVO;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskLogRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.AiAgentTaskService;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
    private final AiUsageLogService aiUsageLogService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final AiModelConvert aiModelConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAgentTaskVO createTask(Long userId, AiAgentTaskCreateRequest request) {
        AiAgentDefinition definition = validateAndGetDefinition(request.getAgentId());
        AiChannelConfig channelConfig = getChannelConfig(definition);

        aiQuotaService.checkQuota(userId, channelConfig);

        AiAgentTask task = new AiAgentTask();
        task.setUserId(userId);
        task.setAgentId(definition.getId());
        task.setStatus(AiAgentTaskStatusEnum.PENDING.getValue());
        task.setInputContent(request.getInputContent());
        task.setTokenCount(0);
        aiAgentTaskRepository.save(task);

        executeTask(task, definition, channelConfig);

        AiAgentTaskVO vo = aiModelConvert.toAgentTaskVO(task);
        vo.setAgentName(definition.getName());
        return vo;
    }

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
    }

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

            recordLogs(task.getId(), task.getInputContent(), result);

            if (result.isSuccess()) {
                aiQuotaService.recordUsage(task.getUserId(), channelConfig.getId());
                aiUsageLogService.logUsage(task.getUserId(), channelConfig.getId(),
                        null, "agent", result.getRequestTokens(),
                        result.getResponseTokens(), result.getTotalTokens(), 1, null);
            } else {
                aiUsageLogService.logUsage(task.getUserId(), channelConfig.getId(),
                        null, "agent", result.getRequestTokens(),
                        result.getResponseTokens(), result.getTotalTokens(), 0, "agent_call_failed");
            }

            notificationDeliveryService.deliverAfterCommit(
                    task.getUserId(),
                    NotificationTypeEnum.AI_TASK_DONE,
                    "Agent 任务完成",
                    definition.getName() + " 任务已完成",
                    null);
        } catch (Exception e) {
            log.error("Agent 任务执行异常: taskId={}", task.getId(), e);
            task.setStatus(AiAgentTaskStatusEnum.FAILED.getValue());
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            aiAgentTaskRepository.updateById(task);
        }
    }

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

    private AiAgentDefinition validateAndGetDefinition(Long agentId) {
        AiAgentDefinition definition = ExceptionThrowerCore.requireNonNull(
                aiAgentDefinitionRepository.getById(agentId),
                ResultErrorCode.AI_AGENT_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(
                definition.getEnabled() != 1,
                ResultErrorCode.AI_AGENT_DISABLED);
        return definition;
    }

    private AiChannelConfig getChannelConfig(AiAgentDefinition definition) {
        return ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(definition.getChannelConfigId()),
                ResultErrorCode.AI_CHANNEL_NOT_FOUND);
    }

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
