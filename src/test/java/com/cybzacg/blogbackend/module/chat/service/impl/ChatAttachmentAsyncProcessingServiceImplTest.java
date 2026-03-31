package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.config.property.ChatAttachmentProcessingProperties;
import com.cybzacg.blogbackend.domain.ChatAttachmentProcessTask;
import com.cybzacg.blogbackend.domain.ChatMessage;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.common.ChatMessagePayloadVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatMessageVO;
import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentMetadataResolver;
import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentProcessTaskService;
import com.cybzacg.blogbackend.module.chat.service.ChatMessageService;
import com.cybzacg.blogbackend.module.chat.service.ChatMetricsService;
import com.cybzacg.blogbackend.module.chat.service.ChatPushService;
import com.cybzacg.blogbackend.module.file.service.FileInfoService;
import com.cybzacg.blogbackend.utils.JsonUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAttachmentAsyncProcessingServiceImplTest {
    @Mock
    private ChatAttachmentProcessTaskService chatAttachmentProcessTaskService;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private FileInfoService fileInfoService;
    @Mock
    private StorageManager storageManager;
    @Mock
    private ChatAttachmentMetadataResolver chatAttachmentMetadataResolver;
    @Mock
    private ChatPushService chatPushService;
    @Mock
    private ChatMetricsService chatMetricsService;

    private ChatAttachmentAsyncProcessingServiceImpl asyncProcessingService;

    @BeforeEach
    void setUp() {
        ChatAttachmentProcessingProperties properties = new ChatAttachmentProcessingProperties();
        properties.setBatchSize(16);
        properties.setMaxRetryCount(3);
        properties.setRetryDelaySeconds(30);
        properties.setLeaseSeconds(300);
        asyncProcessingService = new ChatAttachmentAsyncProcessingServiceImpl(
                Runnable::run,
                chatAttachmentProcessTaskService,
                properties,
                chatMessageService,
                fileInfoService,
                storageManager,
                chatAttachmentMetadataResolver,
                chatPushService,
                chatMetricsService
        );
    }

    @Test
    void scheduleAfterCommitShouldPersistTaskAndGenerateImageThumbnailAndPushMessageUpdated() throws Exception {
        Long messageId = 1001L;
        Long taskId = 9001L;
        InMemoryStorageService storageService = new InMemoryStorageService(Map.of(
                "chat/demo.png", createPngBytes(640, 480)
        ));

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_IMAGE);
        message.setPayloadJson(buildPayloadJson(7001L, "https://example.com/demo.png", ChatConstants.ATTACHMENT_TRANSCODE_STATUS_SOURCE));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(7001L);
        fileInfo.setStorageKey("local");
        fileInfo.setFilePath("chat/demo.png");
        fileInfo.setFileUrl("https://example.com/demo.png");

        ChatMessageVO messageVO = buildMessageSnapshot(messageId, ChatConstants.MESSAGE_TYPE_IMAGE,
                buildFilePayload(7001L, "https://example.com/demo.png", ChatConstants.ATTACHMENT_TRANSCODE_STATUS_SOURCE));
        ChatAttachmentProcessTask task = buildTask(taskId, messageId, ChatConstants.MESSAGE_TYPE_IMAGE, messageVO, List.of(1L, 2L), 0, 3);

        when(chatAttachmentProcessTaskService.saveOrResetPendingTask(
                eq(messageId), eq(ChatConstants.MESSAGE_TYPE_IMAGE), any(String.class), any(String.class), eq(3), any(Date.class)))
                .thenReturn(task);
        when(chatAttachmentProcessTaskService.getById(taskId)).thenReturn(task);
        when(chatAttachmentProcessTaskService.claimTask(eq(taskId), any(Date.class), any(Date.class))).thenReturn(true);
        when(chatAttachmentProcessTaskService.markSuccess(eq(taskId), any(Date.class))).thenReturn(true);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(fileInfoService.getById(7001L)).thenReturn(fileInfo);
        when(storageManager.getStorageService("local")).thenReturn(storageService);
        when(chatMessageService.updateById(any(ChatMessage.class))).thenReturn(true);

        asyncProcessingService.scheduleAfterCommit(messageId, messageVO, List.of(1L, 2L, 2L));

        verify(chatAttachmentProcessTaskService).saveOrResetPendingTask(
                eq(messageId),
                eq(ChatConstants.MESSAGE_TYPE_IMAGE),
                any(String.class),
                argThat(json -> Objects.equals("[1,2]", json)),
                eq(3),
                any(Date.class)
        );
        verify(chatMessageService).updateById(argThat(updated -> {
            ChatMessagePayloadVO payload = JsonUtils.fromJson(updated.getPayloadJson(), ChatMessagePayloadVO.class);
            return payload != null
                    && payload.getFile() != null
                    && Integer.valueOf(640).equals(payload.getFile().getWidth())
                    && Integer.valueOf(480).equals(payload.getFile().getHeight())
                    && Objects.equals("chat/demo__chat_thumb.jpg", payload.getFile().getThumbnailUrl());
        }));
        verify(chatPushService).pushMessageUpdated(argThat(updated ->
                        updated != null
                                && updated.getFile() != null
                                && Integer.valueOf(640).equals(updated.getFile().getWidth())
                                && Integer.valueOf(480).equals(updated.getFile().getHeight())
                                && Objects.equals("chat/demo__chat_thumb.jpg", updated.getFile().getThumbnailUrl())),
                eq(List.of(1L, 2L)));
        verify(chatAttachmentProcessTaskService).markSuccess(eq(taskId), any(Date.class));
        assertTrue(storageService.exists("chat/demo__chat_thumb.jpg"));
    }

    @Test
    void dispatchDueTasksShouldTranscodeVoiceAndPersistWaveform() throws Exception {
        Long messageId = 1002L;
        Long taskId = 9002L;
        InMemoryStorageService storageService = new InMemoryStorageService(Map.of(
                "chat/voice.wav", createWaveBytes()
        ));

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_VOICE);
        message.setPayloadJson(buildPayloadJson(7002L, "https://example.com/voice.wav", ChatConstants.ATTACHMENT_TRANSCODE_STATUS_PENDING));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(7002L);
        fileInfo.setStorageKey("local");
        fileInfo.setFilePath("chat/voice.wav");
        fileInfo.setFileUrl("https://example.com/voice.wav");

        ChatMessageVO messageVO = buildMessageSnapshot(messageId, ChatConstants.MESSAGE_TYPE_VOICE,
                buildFilePayload(7002L, "https://example.com/voice.wav", ChatConstants.ATTACHMENT_TRANSCODE_STATUS_PENDING));
        ChatAttachmentProcessTask task = buildTask(taskId, messageId, ChatConstants.MESSAGE_TYPE_VOICE, messageVO, List.of(2L), 0, 3);

        when(chatAttachmentProcessTaskService.listDispatchableTasks(any(Date.class), eq(16))).thenReturn(List.of(task));
        when(chatAttachmentProcessTaskService.getById(taskId)).thenReturn(task);
        when(chatAttachmentProcessTaskService.claimTask(eq(taskId), any(Date.class), any(Date.class))).thenReturn(true);
        when(chatAttachmentProcessTaskService.markSuccess(eq(taskId), any(Date.class))).thenReturn(true);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(fileInfoService.getById(7002L)).thenReturn(fileInfo);
        when(storageManager.getStorageService("local")).thenReturn(storageService);
        when(chatAttachmentMetadataResolver.resolve(fileInfo, ChatConstants.MESSAGE_TYPE_VOICE))
                .thenReturn(new ChatAttachmentMetadataResolver.ChatAttachmentMetadata(null, null, 12, List.of(5, 15, 25)));
        when(chatMessageService.updateById(any(ChatMessage.class))).thenReturn(true);

        asyncProcessingService.dispatchDueTasks();

        verify(chatMessageService).updateById(argThat(updated -> {
            ChatMessagePayloadVO payload = JsonUtils.fromJson(updated.getPayloadJson(), ChatMessagePayloadVO.class);
            return payload != null
                    && payload.getFile() != null
                    && Integer.valueOf(12).equals(payload.getFile().getDurationSeconds())
                    && List.of(5, 15, 25).equals(payload.getFile().getWaveform())
                    && Objects.equals("chat/voice__chat_preview.wav", payload.getFile().getPreviewUrl())
                    && Objects.equals(ChatConstants.ATTACHMENT_TRANSCODE_STATUS_READY, payload.getFile().getTranscodeStatus());
        }));
        verify(chatPushService).pushMessageUpdated(argThat(updated ->
                        updated != null
                                && updated.getFile() != null
                                && Integer.valueOf(12).equals(updated.getFile().getDurationSeconds())
                                && List.of(5, 15, 25).equals(updated.getFile().getWaveform())
                                && Objects.equals(ChatConstants.ATTACHMENT_TRANSCODE_STATUS_READY, updated.getFile().getTranscodeStatus())),
                eq(List.of(2L)));
        assertTrue(storageService.exists("chat/voice__chat_preview.wav"));
        assertNotNull(storageService.readBytes("chat/voice__chat_preview.wav"));
    }

    @Test
    void dispatchDueTasksShouldHandleLegacyFileOnlyPayload() throws Exception {
        Long messageId = 1003L;
        Long taskId = 9003L;
        InMemoryStorageService storageService = new InMemoryStorageService(Map.of(
                "chat/legacy.png", createPngBytes(320, 160)
        ));

        ChatFilePayloadVO legacyFilePayload = new ChatFilePayloadVO();
        legacyFilePayload.setFileId(7003L);
        legacyFilePayload.setPreviewUrl("https://example.com/legacy.png");
        legacyFilePayload.setThumbnailUrl("https://example.com/legacy.png");

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_IMAGE);
        message.setPayloadJson(JsonUtils.toJson(legacyFilePayload));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(7003L);
        fileInfo.setStorageKey("local");
        fileInfo.setFilePath("chat/legacy.png");
        fileInfo.setFileUrl("https://example.com/legacy.png");

        ChatMessageVO messageVO = buildMessageSnapshot(messageId, ChatConstants.MESSAGE_TYPE_IMAGE, legacyFilePayload);
        ChatAttachmentProcessTask task = buildTask(taskId, messageId, ChatConstants.MESSAGE_TYPE_IMAGE, messageVO, List.of(3L), 0, 3);

        when(chatAttachmentProcessTaskService.listDispatchableTasks(any(Date.class), eq(16))).thenReturn(List.of(task));
        when(chatAttachmentProcessTaskService.getById(taskId)).thenReturn(task);
        when(chatAttachmentProcessTaskService.claimTask(eq(taskId), any(Date.class), any(Date.class))).thenReturn(true);
        when(chatAttachmentProcessTaskService.markSuccess(eq(taskId), any(Date.class))).thenReturn(true);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(fileInfoService.getById(7003L)).thenReturn(fileInfo);
        when(storageManager.getStorageService("local")).thenReturn(storageService);
        when(chatMessageService.updateById(any(ChatMessage.class))).thenReturn(true);

        asyncProcessingService.dispatchDueTasks();

        verify(chatMessageService).updateById(argThat(updated -> {
            ChatMessagePayloadVO payload = JsonUtils.fromJson(updated.getPayloadJson(), ChatMessagePayloadVO.class);
            return payload != null
                    && payload.getFile() != null
                    && Integer.valueOf(320).equals(payload.getFile().getWidth())
                    && Integer.valueOf(160).equals(payload.getFile().getHeight());
        }));
    }

    @Test
    void dispatchDueTasksShouldMarkRetryWhenProcessingFails() {
        Long messageId = 1004L;
        Long taskId = 9004L;
        StorageService failingStorageService = new StorageService() {
            @Override
            public String upload(InputStream inputStream, String objectName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String upload(InputStream inputStream, String objectName, String contentType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream download(String objectName) {
                throw new IllegalStateException("mock download failure");
            }

            @Override
            public boolean delete(String objectName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int deleteBatch(List<String> objectNames) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean exists(String objectName) {
                return false;
            }

            @Override
            public String getUrl(String objectName) {
                return objectName;
            }

            @Override
            public StorageType getStorageType() {
                return StorageType.LOCAL;
            }

            @Override
            public String uploadToTemp(InputStream inputStream, String objectName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String uploadToTemp(InputStream inputStream, String objectName, String contentType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean deleteTempFiles(String uploadId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean deleteTempFilesByPrefix(String prefix) {
                throw new UnsupportedOperationException();
            }
        };

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setMessageType(ChatConstants.MESSAGE_TYPE_IMAGE);
        message.setPayloadJson(buildPayloadJson(7004L, "https://example.com/fail.png", ChatConstants.ATTACHMENT_TRANSCODE_STATUS_SOURCE));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(7004L);
        fileInfo.setStorageKey("local");
        fileInfo.setFilePath("chat/fail.png");

        ChatAttachmentProcessTask task = buildTask(taskId, messageId, ChatConstants.MESSAGE_TYPE_IMAGE,
                buildMessageSnapshot(messageId, ChatConstants.MESSAGE_TYPE_IMAGE,
                        buildFilePayload(7004L, "https://example.com/fail.png", ChatConstants.ATTACHMENT_TRANSCODE_STATUS_SOURCE)),
                List.of(4L),
                0,
                3);

        when(chatAttachmentProcessTaskService.listDispatchableTasks(any(Date.class), eq(16))).thenReturn(List.of(task));
        when(chatAttachmentProcessTaskService.getById(taskId)).thenReturn(task);
        when(chatAttachmentProcessTaskService.claimTask(eq(taskId), any(Date.class), any(Date.class))).thenReturn(true);
        when(chatAttachmentProcessTaskService.markRetry(eq(taskId), eq(1), any(Date.class), any(String.class))).thenReturn(true);
        when(chatMessageService.getById(messageId)).thenReturn(message);
        when(fileInfoService.getById(7004L)).thenReturn(fileInfo);
        when(storageManager.getStorageService("local")).thenReturn(failingStorageService);

        asyncProcessingService.dispatchDueTasks();

        verify(chatAttachmentProcessTaskService).markRetry(eq(taskId), eq(1), any(Date.class), argThat(messageText ->
                messageText != null && messageText.contains("mock download failure")));
        verify(chatAttachmentProcessTaskService, never()).markFailed(eq(taskId), any(Integer.class), any(Date.class), any(String.class));
    }

    @Test
    void recoverExpiredTasksShouldDelegateToTaskService() {
        when(chatAttachmentProcessTaskService.resetExpiredTasks(any(Date.class), eq("processing lease expired"))).thenReturn(2);

        int recovered = asyncProcessingService.recoverExpiredTasks();

        assertEquals(2, recovered);
        verify(chatAttachmentProcessTaskService).resetExpiredTasks(any(Date.class), eq("processing lease expired"));
    }

    private static ChatAttachmentProcessTask buildTask(Long taskId,
                                                       Long messageId,
                                                       String messageType,
                                                       ChatMessageVO messageVO,
                                                       List<Long> pushUserIds,
                                                       int retryCount,
                                                       int maxRetryCount) {
        ChatAttachmentProcessTask task = new ChatAttachmentProcessTask();
        task.setId(taskId);
        task.setMessageId(messageId);
        task.setMessageType(messageType);
        task.setTaskStatus(ChatConstants.ATTACHMENT_TASK_STATUS_PENDING);
        task.setRetryCount(retryCount);
        task.setMaxRetryCount(maxRetryCount);
        task.setMessageSnapshotJson(JsonUtils.toJson(messageVO));
        task.setPushUserIdsJson(JsonUtils.toJson(pushUserIds));
        return task;
    }

    private static ChatMessageVO buildMessageSnapshot(Long messageId, String messageType, ChatFilePayloadVO filePayload) {
        ChatMessageVO messageVO = new ChatMessageVO();
        messageVO.setId(messageId);
        messageVO.setMessageType(messageType);
        messageVO.setFile(filePayload);
        return messageVO;
    }

    private static ChatFilePayloadVO buildFilePayload(Long fileId, String previewUrl, String transcodeStatus) {
        ChatFilePayloadVO filePayload = new ChatFilePayloadVO();
        filePayload.setFileId(fileId);
        filePayload.setPreviewUrl(previewUrl);
        filePayload.setThumbnailUrl(previewUrl);
        filePayload.setTranscodeStatus(transcodeStatus);
        return filePayload;
    }

    private static String buildPayloadJson(Long fileId, String previewUrl, String transcodeStatus) {
        ChatMessagePayloadVO payload = new ChatMessagePayloadVO();
        payload.setFile(buildFilePayload(fileId, previewUrl, transcodeStatus));
        return JsonUtils.toJson(payload);
    }

    private static byte[] createPngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static byte[] createWaveBytes() throws Exception {
        AudioFormat format = new AudioFormat(8000F, 16, 1, true, false);
        int sampleCount = 8000;
        byte[] pcm = new byte[sampleCount * 2];
        for (int index = 0; index < sampleCount; index++) {
            short sample = (short) (Math.sin(index / 12.0D) * 12000);
            pcm[index * 2] = (byte) (sample & 0xFF);
            pcm[index * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pcm);
             AudioInputStream audioInputStream = new AudioInputStream(inputStream, format, sampleCount);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream);
            return outputStream.toByteArray();
        }
    }

    private static final class InMemoryStorageService implements StorageService {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        private InMemoryStorageService(Map<String, byte[]> initialObjects) {
            this.objects.putAll(initialObjects);
        }

        @Override
        public String upload(InputStream inputStream, String objectName) {
            return upload(inputStream, objectName, null);
        }

        @Override
        public String upload(InputStream inputStream, String objectName, String contentType) {
            try {
                objects.put(objectName, inputStream.readAllBytes());
                return objectName;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public InputStream download(String objectName) {
            return new ByteArrayInputStream(readBytes(objectName));
        }

        @Override
        public boolean delete(String objectName) {
            return objects.remove(objectName) != null;
        }

        @Override
        public int deleteBatch(List<String> objectNames) {
            int count = 0;
            for (String objectName : objectNames) {
                if (delete(objectName)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean exists(String objectName) {
            return objects.containsKey(objectName);
        }

        @Override
        public String getUrl(String objectName) {
            return objectName;
        }

        @Override
        public StorageType getStorageType() {
            return StorageType.LOCAL;
        }

        @Override
        public String uploadToTemp(InputStream inputStream, String objectName) {
            return upload(inputStream, objectName);
        }

        @Override
        public String uploadToTemp(InputStream inputStream, String objectName, String contentType) {
            return upload(inputStream, objectName, contentType);
        }

        @Override
        public boolean mergeFiles(List<String> sourceObjectNames, String targetObjectName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteTempFiles(String uploadId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteTempFilesByPrefix(String prefix) {
            throw new UnsupportedOperationException();
        }

        private byte[] readBytes(String objectName) {
            byte[] bytes = objects.get(objectName);
            assertNotNull(bytes);
            return bytes;
        }
    }
}
