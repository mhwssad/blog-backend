package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.storage.MediaAssetPathUtils;
import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.ChatAttachmentProcessingProperties;
import com.cybzacg.blogbackend.domain.ChatAttachmentProcessTask;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.convert.ChatModelMapper;
import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentAsyncProcessingService;
import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentMetadataResolver;
import com.cybzacg.blogbackend.module.chat.repository.ChatAttachmentProcessTaskRepository;
import com.cybzacg.blogbackend.module.chat.repository.ChatMessageRepository;
import com.cybzacg.blogbackend.module.chat.service.ChatMetricsService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 聊天附件异步处理实现。
 *
 * <p>发送消息时先把待处理附件落为持久化任务，事务提交后再异步抢占执行。
 * 调度器会周期性恢复超时租约并继续派发任务，保证节点重启后不会丢失图片缩略图、
 * 语音转码和波形补齐流程。
 */
@Slf4j
@Service
public class ChatAttachmentAsyncProcessingServiceImpl implements ChatAttachmentAsyncProcessingService {
    private static final int IMAGE_THUMBNAIL_MAX_EDGE = 480;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String LEASE_EXPIRED_REASON = "processing lease expired";

    private final java.util.concurrent.Executor asyncTaskExecutor;
    private final ChatAttachmentProcessTaskRepository chatAttachmentProcessTaskRepository;
    private final ChatAttachmentProcessingProperties chatAttachmentProcessingProperties;
    private final ChatMessageRepository chatMessageRepository;
    private final FileInfoRepository fileInfoRepository;
    private final StorageManager storageManager;
    private final ChatAttachmentMetadataResolver chatAttachmentMetadataResolver;
    private final ChatPushService chatPushService;
    private final ChatMetricsService chatMetricsService;
    private final ChatModelMapper chatModelMapper;

    public ChatAttachmentAsyncProcessingServiceImpl(
            @Qualifier("asyncTaskExecutor") java.util.concurrent.Executor asyncTaskExecutor,
            ChatAttachmentProcessTaskRepository chatAttachmentProcessTaskRepository,
            ChatAttachmentProcessingProperties chatAttachmentProcessingProperties,
            ChatMessageRepository chatMessageRepository,
            FileInfoRepository fileInfoRepository,
            StorageManager storageManager,
            ChatAttachmentMetadataResolver chatAttachmentMetadataResolver,
            ChatPushService chatPushService,
            ChatMetricsService chatMetricsService, ChatModelMapper chatModelMapper) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.chatAttachmentProcessTaskRepository = chatAttachmentProcessTaskRepository;
        this.chatAttachmentProcessingProperties = chatAttachmentProcessingProperties;
        this.chatMessageRepository = chatMessageRepository;
        this.fileInfoRepository = fileInfoRepository;
        this.storageManager = storageManager;
        this.chatAttachmentMetadataResolver = chatAttachmentMetadataResolver;
        this.chatPushService = chatPushService;
        this.chatMetricsService = chatMetricsService;
        this.chatModelMapper = chatModelMapper;
    }

    /**
     * 在当前事务提交后调度附件异步处理任务，事务未激活时立即提交。
     *
     * @param messageId       关联的消息 ID
     * @param messageSnapshot 发送时的消息快照，用于异步线程恢复推送数据
     * @param pushUserIds     待推送的目标用户 ID 集合
     */
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

    /**
     * 从数据库批量捞取到期的待处理任务并提交到异步线程池。
     */
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

    /**
     * 重置租约过期但仍处于处理中的任务为待调度状态，避免任务永久卡住。
     *
     * @return 重置的任务数量
     */
    @Override
    public int recoverExpiredTasks() {
        return chatAttachmentProcessTaskRepository.resetExpiredTasks(LocalDateTime.now(), LEASE_EXPIRED_REASON);
    }

    /**
     * 异步提交单个任务到执行线程池，真正执行前仍需先完成数据库抢占。
     */
    private void submitTask(Long taskId) {
        if (taskId == null) {
            return;
        }
        asyncTaskExecutor.execute(() -> processTask(taskId));
    }

    /**
     * 读取并抢占单个持久化任务，避免多个节点同时处理同一条消息。
     */
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

    /**
     * 真正执行媒体处理，并在成功更新 payload 后推送 `message_updated`。
     */
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
            FileInfo fileInfo = fileInfoRepository.getById(payload.getFile().getFileId());
            if (fileInfo == null) {
                markTaskSuccess(task.getId());
                return;
            }

            ChatFilePayloadVO updatedFilePayload = copyFilePayload(payload.getFile());
            boolean changed = switch (message.getMessageType()) {
                case ChatConstants.MESSAGE_TYPE_IMAGE -> enrichImagePayload(updatedFilePayload, fileInfo);
                case ChatConstants.MESSAGE_TYPE_VOICE -> enrichVoicePayload(updatedFilePayload, fileInfo);
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

    private boolean enrichImagePayload(ChatFilePayloadVO payload, FileInfo fileInfo) throws Exception {
        StorageService storageService = resolveStorageService(fileInfo);
        if (storageService == null) {
            return false;
        }
        try (InputStream inputStream = storageService.download(fileInfo.getFilePath())) {
            byte[] sourceBytes = inputStream.readAllBytes();
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceBytes));
            if (sourceImage == null) {
                return false;
            }
            boolean changed = false;
            if (!Objects.equals(payload.getWidth(), sourceImage.getWidth())) {
                payload.setWidth(sourceImage.getWidth());
                changed = true;
            }
            if (!Objects.equals(payload.getHeight(), sourceImage.getHeight())) {
                payload.setHeight(sourceImage.getHeight());
                changed = true;
            }
            String thumbnailUrl = uploadImageThumbnail(storageService, fileInfo, sourceImage);
            if (StrUtils.hasText(thumbnailUrl) && !Objects.equals(payload.getThumbnailUrl(), thumbnailUrl)) {
                payload.setThumbnailUrl(thumbnailUrl);
                changed = true;
            }
            return changed;
        }
    }

    private boolean enrichVoicePayload(ChatFilePayloadVO payload, FileInfo fileInfo) throws Exception {
        StorageService storageService = resolveStorageService(fileInfo);
        if (storageService == null) {
            return false;
        }
        boolean changed = false;
        ChatAttachmentMetadataResolver.ChatAttachmentMetadata metadata =
                chatAttachmentMetadataResolver.resolve(fileInfo, ChatConstants.MESSAGE_TYPE_VOICE);
        if (!Objects.equals(payload.getDurationSeconds(), metadata.durationSeconds())) {
            payload.setDurationSeconds(metadata.durationSeconds());
            changed = true;
        }
        if (!Objects.equals(payload.getWaveform(), metadata.waveform())) {
            payload.setWaveform(metadata.waveform());
            changed = true;
        }
        try (InputStream inputStream = storageService.download(fileInfo.getFilePath())) {
            byte[] previewBytes = buildWavePreviewBytes(inputStream.readAllBytes());
            String previewPath = MediaAssetPathUtils.buildChatVoicePreviewPath(fileInfo.getFilePath());
            String previewUrl = storageService.upload(new ByteArrayInputStream(previewBytes), previewPath, "audio/wav");
            if (StrUtils.hasText(previewUrl) && !Objects.equals(payload.getPreviewUrl(), previewUrl)) {
                payload.setPreviewUrl(previewUrl);
                changed = true;
            }
            if (!Objects.equals(payload.getTranscodeStatus(), ChatConstants.ATTACHMENT_TRANSCODE_STATUS_READY)) {
                payload.setTranscodeStatus(ChatConstants.ATTACHMENT_TRANSCODE_STATUS_READY);
                changed = true;
            }
            return changed;
        } catch (Exception ex) {
            if (!Objects.equals(payload.getTranscodeStatus(), ChatConstants.ATTACHMENT_TRANSCODE_STATUS_FAILED)) {
                payload.setTranscodeStatus(ChatConstants.ATTACHMENT_TRANSCODE_STATUS_FAILED);
                return true;
            }
            throw ex;
        }
    }

    private String uploadImageThumbnail(StorageService storageService, FileInfo fileInfo, BufferedImage sourceImage) throws Exception {
        BufferedImage thumbnail = scaleImage(sourceImage);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(thumbnail, "jpg", outputStream);
            return storageService.upload(
                    new ByteArrayInputStream(outputStream.toByteArray()),
                    MediaAssetPathUtils.buildChatImageThumbnailPath(fileInfo.getFilePath()),
                    "image/jpeg");
        }
    }

    private BufferedImage scaleImage(BufferedImage sourceImage) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int maxEdge = Math.max(sourceWidth, sourceHeight);
        if (maxEdge <= IMAGE_THUMBNAIL_MAX_EDGE) {
            return toRgbImage(sourceImage, sourceWidth, sourceHeight);
        }
        double ratio = IMAGE_THUMBNAIL_MAX_EDGE / (double) maxEdge;
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * ratio));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * ratio));
        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return targetImage;
    }

    private BufferedImage toRgbImage(BufferedImage sourceImage, int width, int height) {
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.drawImage(sourceImage, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }

    /**
     * 当前统一把语音预览转成 WAV，避免前端长期依赖源音频格式差异。
     */
    private byte[] buildWavePreviewBytes(byte[] sourceBytes) throws Exception {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(sourceBytes));
             AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bufferedInputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            AudioFormat targetFormat = buildWaveTargetFormat(sourceStream.getFormat());
            AudioInputStream targetStream = sourceStream;
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(sourceStream.getFormat().getEncoding())
                    || sourceStream.getFormat().getSampleSizeInBits() != 16
                    || sourceStream.getFormat().isBigEndian()) {
                if (!AudioSystem.isConversionSupported(targetFormat, sourceStream.getFormat())) {
                    throw new IllegalStateException("voice transcode to wav is not supported");
                }
                targetStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            }
            try (AudioInputStream closableTargetStream = targetStream) {
                AudioSystem.write(closableTargetStream, AudioFileFormat.Type.WAVE, outputStream);
            }
            return outputStream.toByteArray();
        }
    }

    private AudioFormat buildWaveTargetFormat(AudioFormat sourceFormat) {
        int channels = Math.max(1, sourceFormat.getChannels());
        float sampleRate = sourceFormat.getSampleRate() > 0 ? sourceFormat.getSampleRate() : 16000F;
        int sampleSizeInBits = 16;
        int frameSize = channels * (sampleSizeInBits / 8);
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, channels, frameSize, sampleRate, false);
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
            return chatModelMapper.toMessagePayloadVO(legacyFilePayload);
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

    /**
     * 从任务快照中恢复待推送的消息对象，避免异步线程再次拼装整条消息视图。
     */
    private void pushUpdatedMessage(ChatAttachmentProcessTask task, ChatFilePayloadVO updatedFilePayload, LocalDateTime updatedAt) {
        ChatMessageVO updatedMessage = parseMessageSnapshot(task.getMessageSnapshotJson());
        if (updatedMessage == null) {
            return;
        }
        updatedMessage.setFile(updatedFilePayload);
        updatedMessage.setUpdatedAt(updatedAt);
        chatPushService.pushMessageUpdated(updatedMessage, parsePushUserIds(task.getPushUserIdsJson()));
    }

    /**
     * 任务成功或确认无需继续处理时，统一收口为成功态，避免调度器重复扫描。
     */
    private void markTaskSuccess(Long taskId) {
        if (!chatAttachmentProcessTaskRepository.markSuccess(taskId, LocalDateTime.now())) {
            log.warn("mark chat attachment task success skipped: taskId={}", taskId);
        }
    }

    /**
     * 失败后按指数退避重试；达到上限后转为最终失败，等待人工介入。
     */
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

    /**
     * 构造下次重试时间，使用基础间隔 * 2^(retry-1) 的指数退避，避免错误风暴。
     */
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
