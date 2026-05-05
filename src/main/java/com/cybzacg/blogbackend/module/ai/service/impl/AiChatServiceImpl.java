package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.domain.ai.AiChatMessage;
import com.cybzacg.blogbackend.domain.ai.AiChatSession;
import com.cybzacg.blogbackend.domain.ai.AiMessageAttachment;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.ai.AiChatSessionStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiMessageResponseStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.constant.AiConstants;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.internal.AiRagRetrievalResult;
import com.cybzacg.blogbackend.module.ai.model.user.*;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChatMessageRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChatSessionRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiMessageAttachmentRepository;
import com.cybzacg.blogbackend.module.ai.service.AiChatService;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.module.ai.service.AiRagService;
import com.cybzacg.blogbackend.module.ai.service.AiTokenBudgetService;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private final AiRagService aiRagService;
    private final AiUsageLogService aiUsageLogService;
    private final AiModelConvert aiModelConvert;
    private final AiMessageAttachmentRepository aiMessageAttachmentRepository;
    private final FileInfoRepository fileInfoRepository;
    private final AiTokenBudgetService aiTokenBudgetService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSessionVO createSession(Long userId, AiSessionCreateRequest request) {
        AiChannelConfig config;
        if (request.getChannelConfigId() != null) {
            config = ExceptionThrowerCore.requireNonNull(
                    aiChannelConfigRepository.getById(request.getChannelConfigId()),
                    ResultErrorCode.AI_CHANNEL_NOT_FOUND);
        } else {
            List<AiChannelConfig> enabledChannels = aiChannelConfigRepository.listEnabledOrderByDefault();
            ExceptionThrowerCore.throwBusinessIf(enabledChannels.isEmpty(), ResultErrorCode.AI_CHANNEL_NOT_FOUND);
            config = enabledChannels.get(0);
        }

        ExceptionThrowerCore.throwBusinessIf(
                config.getStatus() == null || config.getStatus() != 1,
                ResultErrorCode.AI_CHANNEL_DISABLED);

        aiQuotaService.checkQuota(userId, config);

        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setChannelConfigId(config.getId());
        session.setTitle(request.getTitle());
        session.setSceneType(request.getSceneType() != null ? request.getSceneType() : AiConstants.SCENE_TYPE_GENERAL);
        session.setStatus(AiChatSessionStatusEnum.NORMAL.getValue());
        session.setLastMessageAt(LocalDateTime.now());
        aiChatSessionRepository.save(session);

        return aiModelConvert.toSessionVO(session);
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
                .map(aiModelConvert::toSessionVO)
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

        AiSessionDetailVO detail = aiModelConvert.toSessionDetailVO(session);
        if (config != null) {
            detail.setChannelName(config.getChannelName());
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
                .map(aiModelConvert::toMessageVO)
                .toList();
        fillAttachments(records);
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

        // 3.5 Token 预算预检
        List<AiChatMessage> preloadedContext = aiChatMessageRepository
                .listBySessionIdOrderById(sessionId, AiConstants.DEFAULT_MAX_CONTEXT_MESSAGES);
        AiTokenBudgetService.BudgetCheck budgetCheck = aiTokenBudgetService.checkBudget(
                config, request.getContent(), preloadedContext, null, null);
        ExceptionThrowerCore.throwBusinessIf(
                !budgetCheck.withinBudget(),
                ResultErrorCode.ILLEGAL_ARGUMENT, "输入内容超出 Token 预算限制");

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

        // 4.5 解析并校验附件
        List<FileInfo> imageAttachments = resolveAttachments(userId, userMessage.getId(), request.getAttachmentFileIds());

        // 5. 复用已加载上下文，按预算裁剪
        List<AiChatMessage> contextMessages = preloadedContext;
        if (config.getMaxHistoryTokens() != null && config.getMaxHistoryTokens() > 0) {
            contextMessages = aiTokenBudgetService.trimHistory(contextMessages, config.getMaxHistoryTokens());
        }

        // 6. RAG 检索并调用 AI 模型
        AiRagRetrievalResult ragResult = aiRagService.retrieve(config, request.getContent());
        String ragContext = ragResult.getContextText();
        if (config.getMaxRagTokens() != null && config.getMaxRagTokens() > 0 && ragContext != null) {
            ragContext = aiTokenBudgetService.trimRagContext(ragContext, config.getMaxRagTokens());
        }
        String systemPrompt = aiRagService.enrichSystemPrompt(config.getSystemPromptTemplate(), ragResult);
        AiModelCallResult callResult;
        AiMessageResponseStatusEnum responseStatus;
        try {
            callResult = aiModelClient.chat(config, systemPrompt, contextMessages,
                    request.getContent(), imageAttachments);
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
        assistantMessage.setRagReferenceJson(ragResult.getReferenceJson());
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
                responseStatus == AiMessageResponseStatusEnum.FAILED ? callResult.getErrorMessage() : null,
                ragResult.isEnabled() ? 1 : 0,
                ragResult.hitCount(),
                ragResult.getDurationMs(),
                ragResult.getReferenceJson()
        );

        // 10. 更新会话最后消息时间
        session.setLastMessageAt(LocalDateTime.now());
        aiChatSessionRepository.updateById(session);

        AiMessageVO result = aiModelConvert.toMessageVO(assistantMessage);
        fillAttachments(List.of(result));
        return result;
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
     * {@inheritDoc}
     */
    @Override
    public SseEmitter streamMessage(Long sessionId, Long userId, AiMessageSendRequest request) {
        // 校验会话归属与状态
        AiChatSession session = verifySessionOwnership(sessionId, userId);
        ExceptionThrowerCore.throwBusinessIf(
                !AiChatSessionStatusEnum.NORMAL.getValue().equals(session.getStatus()),
                ResultErrorCode.AI_SESSION_CLOSED);

        AiChannelConfig config = ExceptionThrowerCore.requireNonNull(
                aiChannelConfigRepository.getById(session.getChannelConfigId()),
                ResultErrorCode.AI_CHANNEL_NOT_FOUND);
        ExceptionThrowerCore.throwBusinessIf(
                config.getStatus() == null || config.getStatus() != 1,
                ResultErrorCode.AI_CHANNEL_DISABLED);

        aiQuotaService.checkQuota(userId, config);

        // 保存用户消息
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setRoleType(AiConstants.ROLE_TYPE_USER);
        userMessage.setContent(request.getContent());
        userMessage.setRequestSceneType(request.getRequestSceneType());
        userMessage.setRequestTargetId(request.getRequestTargetId());
        userMessage.setResponseStatus(AiMessageResponseStatusEnum.SUCCESS.getValue());
        aiChatMessageRepository.save(userMessage);

        List<FileInfo> imageAttachments = resolveAttachments(userId, userMessage.getId(), request.getAttachmentFileIds());

        List<AiChatMessage> contextMessages = aiChatMessageRepository
                .listBySessionIdOrderById(sessionId, AiConstants.DEFAULT_MAX_CONTEXT_MESSAGES);

        SseEmitter emitter = new SseEmitter(AiConstants.DEFAULT_TIMEOUT_SECONDS * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                AiRagRetrievalResult ragResult = aiRagService.retrieve(config, request.getContent());
                String systemPrompt = aiRagService.enrichSystemPrompt(config.getSystemPromptTemplate(), ragResult);

                String fullContent = aiModelClient.streamChat(config, systemPrompt, contextMessages,
                        request.getContent(), imageAttachments,
                        event -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name(event.getType())
                                        .data(JsonUtils.toJson(event)));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        });

                // 流式完成后持久化助手消息
                AiChatMessage assistantMessage = new AiChatMessage();
                assistantMessage.setSessionId(sessionId);
                assistantMessage.setUserId(userId);
                assistantMessage.setRoleType(AiConstants.ROLE_TYPE_ASSISTANT);
                assistantMessage.setContent(fullContent);
                assistantMessage.setRequestSceneType(request.getRequestSceneType());
                assistantMessage.setRequestTargetId(request.getRequestTargetId());
                assistantMessage.setResponseStatus(AiMessageResponseStatusEnum.SUCCESS.getValue());
                assistantMessage.setRagReferenceJson(ragResult.getReferenceJson());
                aiChatMessageRepository.save(assistantMessage);

                aiQuotaService.recordUsage(userId, config.getId());
                aiUsageLogService.logUsage(userId, config.getId(), sessionId,
                        request.getRequestSceneType(), 0, 0, 0,
                        AiMessageResponseStatusEnum.SUCCESS.getValue(), null,
                        ragResult.isEnabled() ? 1 : 0, ragResult.hitCount(),
                        ragResult.getDurationMs(), ragResult.getReferenceJson());

                session.setLastMessageAt(LocalDateTime.now());
                aiChatSessionRepository.updateById(session);
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 流式异常, sessionId={}, userId={}: {}", sessionId, userId, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> log.warn("SSE 流式超时, sessionId={}", sessionId));
        emitter.onError(e -> log.warn("SSE 连接异常, sessionId={}: {}", sessionId, e.getMessage()));

        return emitter;
    }

    /**
     * 校验会话归属关系。
     */
    private AiChatSession verifySessionOwnership(Long sessionId, Long userId) {
        AiChatSession session = aiChatSessionRepository.findByIdAndUserId(sessionId, userId);
        ExceptionThrowerCore.throwBusinessIfNull(session, ResultErrorCode.AI_SESSION_NOT_FOUND);
        return session;
    }

    /**
     * 解析并校验附件文件，保存关联记录，返回图片类附件列表。
     */
    private List<FileInfo> resolveAttachments(Long userId, Long messageId, List<Long> attachmentFileIds) {
        if (attachmentFileIds == null || attachmentFileIds.isEmpty()) {
            return List.of();
        }

        List<FileInfo> files = fileInfoRepository.listByIds(attachmentFileIds);
        List<FileInfo> validFiles = files.stream()
                .filter(f -> userId.equals(f.getUploadUserId()))
                .filter(f -> f.getStatus() != null && f.getStatus() == 1)
                .filter(f -> AiConstants.ALLOWED_ATTACHMENT_FILE_TYPES.contains(f.getFileType()))
                .limit(AiConstants.MAX_ATTACHMENTS_PER_MESSAGE)
                .toList();

        ExceptionThrowerCore.throwBusinessIf(
                validFiles.size() != attachmentFileIds.size(),
                ResultErrorCode.ILLEGAL_ARGUMENT, "部分附件不存在、无权限或不支持该文件类型");

        List<AiMessageAttachment> attachments = validFiles.stream()
                .map(f -> {
                    AiMessageAttachment a = new AiMessageAttachment();
                    a.setMessageId(messageId);
                    a.setFileId(f.getId());
                    a.setFileType(f.getFileType());
                    a.setMimeType(f.getMimeType());
                    return a;
                }).toList();
        aiMessageAttachmentRepository.saveBatch(attachments);

        return validFiles.stream()
                .filter(f -> "image".equals(f.getFileType()))
                .toList();
    }

    /**
     * 批量填充消息 VO 的附件信息。
     */
    private void fillAttachments(List<AiMessageVO> messages) {
        if (messages.isEmpty()) {
            return;
        }
        List<Long> messageIds = messages.stream().map(AiMessageVO::getId).toList();
        List<AiMessageAttachment> allAttachments = aiMessageAttachmentRepository.listByMessageIds(messageIds);
        if (allAttachments.isEmpty()) {
            return;
        }

        // 批量查询文件信息以填充 fileUrl
        List<Long> fileIds = allAttachments.stream().map(AiMessageAttachment::getFileId).distinct().toList();
        Map<Long, FileInfo> fileInfoMap = fileInfoRepository.listByIds(fileIds).stream()
                .collect(java.util.stream.Collectors.toMap(FileInfo::getId, f -> f));

        Map<Long, List<AiMessageAttachment>> grouped = allAttachments.stream()
                .collect(java.util.stream.Collectors.groupingBy(AiMessageAttachment::getMessageId));

        for (AiMessageVO vo : messages) {
            List<AiMessageAttachment> msgAttachments = grouped.getOrDefault(vo.getId(), List.of());
            if (msgAttachments.isEmpty()) {
                continue;
            }
            List<AttachmentVO> attachmentVOs = msgAttachments.stream()
                    .map(a -> {
                        AttachmentVO avo = aiModelConvert.toAttachmentVO(a);
                        FileInfo fi = fileInfoMap.get(a.getFileId());
                        if (fi != null) {
                            avo.setFileUrl(fi.getFileUrl() != null ? fi.getFileUrl() : fi.getFilePath());
                        }
                        return avo;
                    }).toList();
            vo.setAttachments(attachmentVOs);
        }
    }
}
