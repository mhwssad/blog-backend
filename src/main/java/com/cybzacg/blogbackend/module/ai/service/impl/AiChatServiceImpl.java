package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.enums.ai.AiChatSessionStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiMessageResponseStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.constant.AiConstants;
import com.cybzacg.blogbackend.module.ai.convert.AiModelMapper;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.user.*;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChatMessageRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChatSessionRepository;
import com.cybzacg.blogbackend.module.ai.service.AiChatService;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 对话服务实现。
 *
 * <p>负责会话创建、消息收发、上下文管理、AI 模型调用及额度扣减。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AiChannelConfigRepository aiChannelConfigRepository;
    private final AiModelClient aiModelClient;
    private final AiQuotaService aiQuotaService;
    private final AiUsageLogService aiUsageLogService;
    private final AiModelMapper aiModelMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSessionVO createSession(Long userId, AiSessionCreateRequest request) {
        // 确定渠道配置
        AiChannelConfig config;
        if (request.getChannelConfigId() != null) {
            config = ExceptionThrowerCore.requireNonNull(
                    aiChannelConfigRepository.getById(request.getChannelConfigId()),
                    ResultErrorCode.AI_CHANNEL_NOT_FOUND);
        } else {
            // 使用默认渠道
            List<AiChannelConfig> enabledChannels = aiChannelConfigRepository.listEnabledOrderByDefault();
            ExceptionThrowerCore.throwBusinessIf(enabledChannels.isEmpty(), ResultErrorCode.AI_CHANNEL_NOT_FOUND);
            config = enabledChannels.get(0);
        }

        // 校验渠道状态
        ExceptionThrowerCore.throwBusinessIf(
                config.getStatus() == null || config.getStatus() != 1,
                ResultErrorCode.AI_CHANNEL_DISABLED);

        // 预检查额度
        aiQuotaService.checkQuota(userId, config);

        // 创建会话
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setChannelConfigId(config.getId());
        session.setTitle(request.getTitle());
        session.setSceneType(request.getSceneType() != null ? request.getSceneType() : AiConstants.SCENE_TYPE_GENERAL);
        session.setStatus(AiChatSessionStatusEnum.NORMAL.getValue());
        session.setLastMessageAt(LocalDateTime.now());
        aiChatSessionRepository.save(session);

        return aiModelMapper.toSessionVO(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AiSessionVO> listMySessions(Long userId, long current, long size) {
        current = PaginationUtils.normalizeCurrent(current);
        size = PaginationUtils.normalizeSize(size, 10L, 50L);

        Page<AiChatSession> page = aiChatSessionRepository.pageByUserIdAndStatus(
                userId, AiChatSessionStatusEnum.NORMAL.getValue(), current, size);

        List<AiSessionVO> records = page.getRecords().stream()
                .map(aiModelMapper::toSessionVO)
                .toList();

        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AiSessionDetailVO getSessionDetail(Long sessionId, Long userId) {
        AiChatSession session = verifySessionOwnership(sessionId, userId);
        AiChannelConfig config = aiChannelConfigRepository.getById(session.getChannelConfigId());

        AiSessionDetailVO detail = aiModelMapper.toSessionDetailVO(session);
        if (config != null) {
            detail.setChannelName(config.getChannelName());
            detail.setModelName(config.getModelName());
        }
        return detail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AiMessageVO> listMessages(Long sessionId, Long userId, long current, long size) {
        verifySessionOwnership(sessionId, userId);
        current = PaginationUtils.normalizeCurrent(current);
        size = PaginationUtils.normalizeSize(size, 20L, 100L);

        Page<AiChatMessage> page = aiChatMessageRepository.pageBySessionId(sessionId, current, size);
        List<AiMessageVO> records = page.getRecords().stream()
                .map(aiModelMapper::toMessageVO)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMessageVO sendMessage(Long sessionId, Long userId, AiMessageSendRequest request) {
        // 1. 校验会话归属与状态
        AiChatSession session = verifySessionOwnership(sessionId, userId);
        ExceptionThrowerCore.throwBusinessIf(
                !AiChatSessionStatusEnum.NORMAL.getValue().equals(session.getStatus()),
                ResultErrorCode.AI_SESSION_CLOSED);

        // 2. 加载渠道配置
        AiChannelConfig config = ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(session.getChannelConfigId()),
                ResultErrorCode.AI_CHANNEL_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(
                config.getStatus() == null || config.getStatus() != 1,
                ResultErrorCode.AI_CHANNEL_DISABLED);

        // 3. 额度检查
        aiQuotaService.checkQuota(userId, config);

        // 4. 保存用户消息
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setRoleType(AiConstants.ROLE_TYPE_USER);
        userMessage.setContent(request.getContent());
        userMessage.setRequestSceneType(request.getRequestSceneType());
        userMessage.setRequestTargetId(request.getRequestTargetId());
        userMessage.setResponseStatus(AiMessageResponseStatusEnum.SUCCESS.getValue());
        aiChatMessageRepository.save(userMessage);

        // 5. 加载上下文消息
        List<AiChatMessage> contextMessages = aiChatMessageRepository
                .listBySessionIdOrderById(sessionId, AiConstants.DEFAULT_MAX_CONTEXT_MESSAGES);

        // 6. 调用 AI 模型
        AiModelCallResult callResult;
        AiMessageResponseStatusEnum responseStatus;
        try {
            callResult = aiModelClient.chat(config, config.getSystemPromptTemplate(), contextMessages, request.getContent());
            responseStatus = callResult.isSuccess()
                    ? AiMessageResponseStatusEnum.SUCCESS
                    : AiMessageResponseStatusEnum.FAILED;
        } catch (Exception e) {
            log.error("AI模型调用异常, sessionId={}, userId={}: {}", sessionId, userId, e.getMessage(), e);
            callResult = new AiModelCallResult();
            callResult.setSuccess(false);
            callResult.setErrorMessage(e.getMessage());
            responseStatus = AiMessageResponseStatusEnum.FAILED;
        }

        // 7. 保存助手消息
        AiChatMessage assistantMessage = new AiChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setUserId(userId);
        assistantMessage.setRoleType(AiConstants.ROLE_TYPE_ASSISTANT);
        assistantMessage.setContent(callResult.getContent());
        assistantMessage.setRequestSceneType(request.getRequestSceneType());
        assistantMessage.setRequestTargetId(request.getRequestTargetId());
        assistantMessage.setTokenCount(callResult.getTotalTokens());
        assistantMessage.setResponseStatus(responseStatus.getValue());
        assistantMessage.setErrorMessage(responseStatus == AiMessageResponseStatusEnum.FAILED
                ? callResult.getErrorMessage() : null);
        aiChatMessageRepository.save(assistantMessage);

        // 8. 记录额度使用
        aiQuotaService.recordUsage(userId, config.getId());

        // 9. 记录使用日志
        aiUsageLogService.logUsage(
                userId,
                config.getId(),
                sessionId,
                request.getRequestSceneType(),
                callResult.getRequestTokens(),
                callResult.getResponseTokens(),
                callResult.getTotalTokens(),
                responseStatus.getValue(),
                responseStatus == AiMessageResponseStatusEnum.FAILED ? callResult.getErrorMessage() : null
        );

        // 10. 更新会话最后消息时间
        session.setLastMessageAt(LocalDateTime.now());
        aiChatSessionRepository.updateById(session);

        return aiModelMapper.toMessageVO(assistantMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        AiChatSession session = verifySessionOwnership(sessionId, userId);
        session.setStatus(AiChatSessionStatusEnum.CLOSED.getValue());
        session.setUpdatedAt(LocalDateTime.now());
        aiChatSessionRepository.updateById(session);
    }

    /**
     * 校验会话归属关系。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 会话实体
     */
    private AiChatSession verifySessionOwnership(Long sessionId, Long userId) {
        AiChatSession session = aiChatSessionRepository.findByIdAndUserId(sessionId, userId);
        ExceptionThrowerCore.throwBusinessIfNull(session, ResultErrorCode.AI_SESSION_NOT_FOUND);
        return session;
    }
}
