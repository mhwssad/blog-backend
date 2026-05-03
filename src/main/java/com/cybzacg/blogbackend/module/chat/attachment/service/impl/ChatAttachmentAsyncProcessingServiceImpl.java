package com.cybzacg.blogbackend.module.chat.attachment.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.ChatAttachmentProcessingProperties;
import com.cybzacg.blogbackend.domain.chat.ChatAttachmentProcessTask;
import com.cybzacg.blogbackend.domain.chat.ChatMessage;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.chat.attachment.repository.ChatAttachmentProcessTaskRepository;
import com.cybzacg.blogbackend.module.chat.attachment.service.ChatAttachmentAsyncProcessingService;
import com.cybzacg.blogbackend.module.chat.attachment.service.ChatAttachmentMetadataResolver;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatMetricsService;
import com.cybzacg.blogbackend.module.chat.message.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.message.repository.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.push.service.ChatPushService;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.shared.convert.ChatModelConvert;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.shared.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.file.service.FileChatFacadeService;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 聊天附件异步处理门面。<p>负责任务调度、派发和生命周期管理，图片和语音处理委托给对应子处理器。</p>
 */
@Slf4j
@Service
public class ChatAttachmentAsyncProcessingServiceImpl implements ChatAttachmentAsyncProcessingService {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String LEASE_EXPIRED_REASON = "processing lease expired";

    private final java.util.concurrent.Executor asyncTaskExecutor;
    private final ChatAttachmentProcessTaskRepository chatAttachmentProcessTaskRepository;
    private final ChatAttachmentProcessingProperties chatAttachmentProcessingProperties;
    private final ChatMessageRepository chatMessageRepository;
    private final FileChatFacadeService fileChatFacadeService;
    private final StorageManager storageManager;
    private final ChatAttachmentMetadataResolver chatAttachmentMetadataResolver;
    private final ChatPushService chatPushService;
    private final ChatMetricsService chatMetricsService;
    private final ChatModelConvert chatModelConvert;
    private final ChatAttachmentImageProcessor imageProcessor;
    private final ChatAttachmentVoiceProcessor voiceProcessor;

    public ChatAttachmentAsyncProcessingServiceImpl(
            @Qualifier("asyncTaskExecutor") java.util.concurrent.Executor asyncTaskExecutor,
            ChatAttachmentProcessTaskRepository chatAttachmentProcessTaskRepository,
            ChatAttachmentProcessingProperties chatAttachmentProcessingProperties,
            ChatMessageRepository chatMessageRepository,
            FileChatFacadeService fileChatFacadeService,
            StorageManager storageManager,
            ChatAttachmentMetadataResolver chatAttachmentMetadataResolver,
            ChatPushService chatPushService,
            ChatMetricsService chatMetricsService, ChatModelConvert chatModelConvert,
            ChatAttachmentImageProcessor imageProcessor,
            ChatAttachmentVoiceProcessor voiceProcessor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.chatAttachmentProcessTaskRepository = chatAttachmentProcessTaskRepository;
        this.chatAttachmentProcessingProperties = chatAttachmentProcessingProperties;
        this.chatMessageRepository = chatMessageRepository;
        this.fileChatFacadeService = fileChatFacadeService;
        this.storageManager = storageManager;
        this.chatAttachmentMetadataResolver = chatAttachmentMetadataResolver;
        this.chatPushService = chatPushService;
        this.chatMetricsService = chatMetricsService;
        this.chatModelConvert = chatModelConvert;
        this.imageProcessor = imageProcessor;
        this.voiceProcessor = voiceProcessor;
    }

    @Override
    public void scheduleAfterCommit(Long messageId, ChatMessageVO messageSnapshot, Collection<Long> pushUserIds) {
        if (messageId == null
                || messageSnapshot == null
                || !isSupportedAsyncMessageType(messageSnapshot.getMessageType())) {
            return;
        }
        ChatAttachmentProcessTask task = chatAttachmentProcessTaskRepository.saveOrResetPendingTask(
                messageId,
                messageSnapshot.getMessageType(),
                serializeMessageSnapshot(messageSnapshot),
                serializePushUserIds(pushUserIds),
                chatAttachmentProcessingProperties.getMaxRetryCount(),
                LocalDateTime.now()
        );
        Runnable scheduleAction = () -> submitTask(task.getId());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scheduleAction.run();
                }
            });
            return;
        }
        scheduleAction.run();
    }

    @Override
    public void dispatchDueTasks() {
        List<ChatAttachmentProcessTask> tasks = chatAttachmentProcessTaskRepository.listDispatchableTasks(
                LocalDateTime.now(),
                chatAttachmentProcessingProperties.getBatchSize()
        );
        for (ChatAttachmentProcessTask task : tasks) {
            submitTask(task.getId());
        }
    }

    @Override
    public int recoverExpiredTasks() {
        return chatAttachmentProcessTaskRepository.resetExpiredTasks(LocalDateTime.now(), LEASE_EXPIRED_REASON);
    }

    private void submitTask(Long taskId) {
        if (taskId == null) {
            return;
        }
        asyncTaskExecutor.execute(() -> processTask(taskId));
    }

    private void processTask(Long taskId) {
        ChatAttachmentProcessTask task = chatAttachmentProcessTaskRepository.getById(taskId);
        if (task == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaseExpireAt = now.plusSeconds(chatAttachmentProcessingProperties.getLeaseSeconds());
        if (!chatAttachmentProcessTaskRepository.claimTask(taskId, now, leaseExpireAt)) {
            return;
        }
        processClaimedTask(task);
    }

    private void processClaimedTask(ChatAttachmentProcessTask task) {
        long startNanos = System.nanoTime();
        String messageType = task.getMessageType();
        String result = "skipped";
        try {
            ChatMessage message = chatMessageRepository.getById(task.getMessageId());
            if (message == null
                    || !isSupportedAsyncMessageType(message.getMessageType())
                    || Objects.equals(message.getRevokeStatus(), ChatConstants.REVOKE_STATUS_REVOKED)) {
                markTaskSuccess(task.getId());
                return;
            }
            messageType = message.getMessageType();
            ChatMessagePayloadVO payload = parsePayload(message.getPayloadJson());
            if (payload == null || payload.getFile() == null || payload.getFile().getFileId() == null) {
                markTaskSuccess(task.getId());
                return;
            }
            FileInfo fileInfo = fileChatFacadeService.getFileInfo(payload.getFile().getFileId());
            if (fileInfo == null) {
                markTaskSuccess(task.getId());
                return;
            }

            StorageService storageService = resolveStorageService(fileInfo);
            if (storageService == null) {
                markTaskSuccess(task.getId());
                return;
            }

            ChatFilePayloadVO updatedFilePayload = copyFilePayload(payload.getFile());
            boolean changed = switch (message.getMessageType()) {
                case ChatConstants.MESSAGE_TYPE_IMAGE -> imageProcessor.enrichImagePayload(updatedFilePayload, storageService, fileInfo);
                case ChatConstants.MESSAGE_TYPE_VOICE -> voiceProcessor.enrichVoicePayload(updatedFilePayload, storageService, fileInfo, chatAttachmentMetadataResolver);
                default -> false;
            };
            if (!changed) {
                markTaskSuccess(task.getId());
                return;
            }

            payload.setFile(updatedFilePayload);
            message.setPayloadJson(JsonUtils.toJson(payload));
            if (!chatMessageRepository.updateById(message)) {
                throw new IllegalStateException("update chat attachment payload failed");
            }

            pushUpdatedMessage(task, updatedFilePayload, message.getUpdatedAt());
            markTaskSuccess(task.getId());
            result = "success";
        } catch (Exception ex) {
            result = "failed";
            markTaskFailure(task, ex);
            log.warn("process chat attachment async task failed: taskId={}, messageId={}, type={}",
                    task.getId(), task.getMessageId(), messageType, ex);
        } finally {
            chatMetricsService.recordMediaProcess(messageType, result, System.nanoTime() - startNanos);
        }
    }

    private StorageService resolveStorageService(FileInfo fileInfo) {
        if (fileInfo == null || !StrUtils.hasText(fileInfo.getStorageKey()) || !StrUtils.hasText(fileInfo.getFilePath())) {
            return null;
        }
        return storageManager.getStorageService(fileInfo.getStorageKey());
    }

    private boolean isSupportedAsyncMessageType(String messageType) {
        return Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE)
                || Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE);
    }

    private ChatMessagePayloadVO parsePayload(String payloadJson) {
        if (!StrUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            ChatMessagePayloadVO payload = JsonUtils.fromJson(payloadJson, ChatMessagePayloadVO.class);
            if (payload != null && payload.getFile() != null) {
                return payload;
            }
            ChatFilePayloadVO legacyFilePayload = JsonUtils.fromJson(payloadJson, ChatFilePayloadVO.class);
            if (legacyFilePayload == null || legacyFilePayload.getFileId() == null) {
                return payload;
            }
            return chatModelConvert.toMessagePayloadVO(legacyFilePayload);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ChatFilePayloadVO copyFilePayload(ChatFilePayloadVO payload) {
        if (payload == null) {
            return null;
        }
        return JsonUtils.getObjectMapper().convertValue(payload, ChatFilePayloadVO.class);
    }

    private void pushUpdatedMessage(ChatAttachmentProcessTask task, ChatFilePayloadVO updatedFilePayload, LocalDateTime updatedAt) {
        ChatMessageVO updatedMessage = parseMessageSnapshot(task.getMessageSnapshotJson());
        if (updatedMessage == null) {
            return;
        }
        updatedMessage.setFile(updatedFilePayload);
        updatedMessage.setUpdatedAt(updatedAt);
        chatPushService.pushMessageUpdated(updatedMessage, parsePushUserIds(task.getPushUserIdsJson()));
    }

    private void markTaskSuccess(Long taskId) {
        if (!chatAttachmentProcessTaskRepository.markSuccess(taskId, LocalDateTime.now())) {
            log.warn("mark chat attachment task success skipped: taskId={}", taskId);
        }
    }

    private void markTaskFailure(ChatAttachmentProcessTask task, Exception ex) {
        int currentRetryCount = Objects.requireNonNullElse(task.getRetryCount(), 0) + 1;
        int maxRetryCount = Objects.requireNonNullElse(
                task.getMaxRetryCount(),
                chatAttachmentProcessingProperties.getMaxRetryCount()
        );
        String errorMessage = abbreviateError(ex);
        LocalDateTime now = LocalDateTime.now();
        boolean updated;
        if (currentRetryCount >= maxRetryCount) {
            updated = chatAttachmentProcessTaskRepository.markFailed(task.getId(), currentRetryCount, now, errorMessage);
        } else {
            updated = chatAttachmentProcessTaskRepository.markRetry(
                    task.getId(),
                    currentRetryCount,
                    buildNextRetryAt(now, currentRetryCount),
                    errorMessage
            );
        }
        if (!updated) {
            log.warn("mark chat attachment task failure state skipped: taskId={}, retryCount={}", task.getId(), currentRetryCount);
        }
    }

    private LocalDateTime buildNextRetryAt(LocalDateTime now, int retryCount) {
        long multiplier = 1L << Math.min(Math.max(retryCount - 1, 0), 4);
        long delaySeconds = chatAttachmentProcessingProperties.getRetryDelaySeconds() * multiplier;
        return now.plusSeconds(delaySeconds);
    }

    private String serializeMessageSnapshot(ChatMessageVO messageSnapshot) {
        return messageSnapshot == null ? null : JsonUtils.toJson(messageSnapshot);
    }

    private String serializePushUserIds(Collection<Long> pushUserIds) {
        return JsonUtils.toJson(distinctUserIds(pushUserIds));
    }

    private ChatMessageVO parseMessageSnapshot(String snapshotJson) {
        if (!StrUtils.hasText(snapshotJson)) {
            return null;
        }
        try {
            return JsonUtils.fromJson(snapshotJson, ChatMessageVO.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private List<Long> parsePushUserIds(String pushUserIdsJson) {
        if (!StrUtils.hasText(pushUserIdsJson)) {
            return List.of();
        }
        try {
            return distinctUserIds(JsonUtils.fromJson(pushUserIdsJson, new TypeReference<List<Long>>() {
            }));
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private String abbreviateError(Exception ex) {
        String message = ex == null ? null : StrUtils.trimToNull(ex.getMessage());
        if (!StrUtils.hasText(message)) {
            message = ex == null ? "unknown error" : ex.getClass().getSimpleName();
        }
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private List<Long> distinctUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                result.add(userId);
            }
        }
        return List.copyOf(result);
    }
}
