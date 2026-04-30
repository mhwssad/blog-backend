package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.enums.storage.StorageType;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.attachment.service.ChatAttachmentMetadataResolver;
import com.cybzacg.blogbackend.module.chat.attachment.service.impl.ChatAttachmentMetadataResolverImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAttachmentMetadataResolverImplTest {
    @Mock
    private StorageManager storageManager;

    private ChatAttachmentMetadataResolverImpl resolver;

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

    @BeforeEach
    void setUp() {
        resolver = new ChatAttachmentMetadataResolverImpl(storageManager);
    }

    @Test
    void resolveShouldReadImageDimensions() throws Exception {
        when(storageManager.getStorageService("local")).thenReturn(new InMemoryStorageService(Map.of(
                "chat/demo.png", createPngBytes(40, 20)
        )));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setStorageKey("local");
        fileInfo.setFilePath("chat/demo.png");

        ChatAttachmentMetadataResolver.ChatAttachmentMetadata metadata = resolver.resolve(fileInfo, ChatConstants.MESSAGE_TYPE_IMAGE);

        assertEquals(40, metadata.width());
        assertEquals(20, metadata.height());
    }

    @Test
    void resolveShouldReadVoiceDurationAndWaveform() throws Exception {
        when(storageManager.getStorageService("local")).thenReturn(new InMemoryStorageService(Map.of(
                "chat/voice.wav", createWaveBytes()
        )));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setStorageKey("local");
        fileInfo.setFilePath("chat/voice.wav");

        ChatAttachmentMetadataResolver.ChatAttachmentMetadata metadata = resolver.resolve(fileInfo, ChatConstants.MESSAGE_TYPE_VOICE);

        assertNotNull(metadata.durationSeconds());
        assertTrue(metadata.durationSeconds() >= 1);
        assertNotNull(metadata.waveform());
        assertTrue(!metadata.waveform().isEmpty());
    }

    private static class InMemoryStorageService implements StorageService {
        private final Map<String, byte[]> objects;

        private InMemoryStorageService(Map<String, byte[]> objects) {
            this.objects = objects;
        }

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
            return new ByteArrayInputStream(objects.get(objectName));
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
    }
}
