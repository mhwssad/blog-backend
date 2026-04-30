package com.cybzacg.blogbackend.module.chat.attachment.service.impl;

import com.cybzacg.blogbackend.common.storage.StorageManager;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.module.chat.attachment.service.ChatAttachmentMetadataResolver;
import com.cybzacg.blogbackend.module.chat.shared.constant.ChatConstants;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 聊天附件元数据解析实现。
 *
 * <p>当前采用“发送时最佳努力解析”的策略：
 * 图片优先解析宽高，语音优先解析时长和基础波形；失败时回落为空，不阻塞消息发送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAttachmentMetadataResolverImpl implements ChatAttachmentMetadataResolver {
    private static final int WAVEFORM_POINT_COUNT = 24;

    private final StorageManager storageManager;

    /**
     * 根据消息类型解析附件元数据（图片宽高、语音时长和波形）。
     *
     * @param fileInfo    文件实体
     * @param messageType 消息类型，如 image 或 voice
     * @return 解析到的元数据，解析失败时返回空元数据
     */
    @Override
    public ChatAttachmentMetadata resolve(FileInfo fileInfo, String messageType) {
        StorageService storageService = resolveStorageService(fileInfo);
        if (storageService == null) {
            return ChatAttachmentMetadata.empty();
        }
        if (Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_IMAGE)) {
            return resolveImageMetadata(storageService, fileInfo);
        }
        if (Objects.equals(messageType, ChatConstants.MESSAGE_TYPE_VOICE)) {
            return resolveVoiceMetadata(storageService, fileInfo);
        }
        return ChatAttachmentMetadata.empty();
    }

    /**
     * 图片消息只需要补齐基础宽高；缩略图仍由后续独立链路负责。
     */
    private ChatAttachmentMetadata resolveImageMetadata(StorageService storageService, FileInfo fileInfo) {
        try (InputStream inputStream = storageService.download(fileInfo.getFilePath())) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                return ChatAttachmentMetadata.empty();
            }
            return new ChatAttachmentMetadata(image.getWidth(), image.getHeight(), null, null);
        } catch (Exception ex) {
            log.warn("resolve chat image metadata failed: fileId={}, storageKey={}, path={}",
                    fileInfo.getId(), fileInfo.getStorageKey(), fileInfo.getFilePath(), ex);
            return ChatAttachmentMetadata.empty();
        }
    }

    /**
     * 语音消息当前优先兼容 Java 原生可识别音频格式，尽力补齐时长和波形。
     */
    private ChatAttachmentMetadata resolveVoiceMetadata(StorageService storageService, FileInfo fileInfo) {
        try (InputStream inputStream = storageService.download(fileInfo.getFilePath());
             BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
             AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bufferedStream)) {
            Integer durationSeconds = resolveDurationSeconds(sourceStream);
            List<Integer> waveform = resolveWaveform(sourceStream);
            return new ChatAttachmentMetadata(null, null, durationSeconds, waveform);
        } catch (Exception ex) {
            log.warn("resolve chat voice metadata failed: fileId={}, storageKey={}, path={}",
                    fileInfo.getId(), fileInfo.getStorageKey(), fileInfo.getFilePath(), ex);
            return ChatAttachmentMetadata.empty();
        }
    }

    /**
     * 通过音频帧数换算基础时长；未知帧长时回落为空。
     */
    private Integer resolveDurationSeconds(AudioInputStream sourceStream) {
        long frameLength = sourceStream.getFrameLength();
        float frameRate = sourceStream.getFormat().getFrameRate();
        if (frameLength <= 0 || frameRate <= 0) {
            return null;
        }
        return (int) Math.max(1L, (long) Math.ceil(frameLength / (double) frameRate));
    }

    /**
     * 当前只对 PCM 或可转换到 PCM 的音频计算基础波形采样点。
     */
    private List<Integer> resolveWaveform(AudioInputStream sourceStream) throws Exception {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioInputStream waveformStream = sourceStream;
        if (!AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding()) || sourceFormat.getSampleSizeInBits() <= 0) {
            AudioFormat targetFormat = buildPcmTargetFormat(sourceFormat);
            if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                return null;
            }
            waveformStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            sourceFormat = targetFormat;
        }
        byte[] audioBytes = waveformStream.readAllBytes();
        return sampleWaveform(audioBytes, sourceFormat);
    }

    private AudioFormat buildPcmTargetFormat(AudioFormat sourceFormat) {
        int channels = Math.max(1, sourceFormat.getChannels());
        float sampleRate = sourceFormat.getSampleRate() > 0 ? sourceFormat.getSampleRate() : 8000F;
        int sampleSizeInBits = 16;
        int frameSize = channels * (sampleSizeInBits / 8);
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, channels, frameSize, sampleRate, false);
    }

    /**
     * 按固定桶数抽样，输出 0-100 的简化波形强度，供前端基础展示使用。
     */
    private List<Integer> sampleWaveform(byte[] audioBytes, AudioFormat format) throws IOException {
        if (audioBytes == null || audioBytes.length == 0) {
            return null;
        }
        int frameSize = Math.max(1, format.getFrameSize());
        int sampleSizeInBytes = Math.max(1, format.getSampleSizeInBits() / 8);
        int channels = Math.max(1, format.getChannels());
        int totalFrames = audioBytes.length / frameSize;
        if (totalFrames <= 0) {
            return null;
        }
        int bucketSize = Math.max(1, totalFrames / WAVEFORM_POINT_COUNT);
        long maxAmplitude = (1L << (sampleSizeInBytes * 8 - 1)) - 1;
        List<Integer> points = new ArrayList<>(WAVEFORM_POINT_COUNT);
        for (int bucketIndex = 0; bucketIndex < WAVEFORM_POINT_COUNT; bucketIndex++) {
            int startFrame = bucketIndex * bucketSize;
            if (startFrame >= totalFrames) {
                break;
            }
            int endFrame = bucketIndex == WAVEFORM_POINT_COUNT - 1 ? totalFrames : Math.min(totalFrames, startFrame + bucketSize);
            long amplitudeSum = 0L;
            int sampleCount = 0;
            for (int frameIndex = startFrame; frameIndex < endFrame; frameIndex++) {
                int frameOffset = frameIndex * frameSize;
                for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                    int sampleOffset = frameOffset + channelIndex * sampleSizeInBytes;
                    if (sampleOffset + sampleSizeInBytes > audioBytes.length) {
                        continue;
                    }
                    amplitudeSum += Math.abs(decodeSignedSample(audioBytes, sampleOffset, sampleSizeInBytes, format.isBigEndian()));
                    sampleCount++;
                }
            }
            int value = sampleCount == 0 || maxAmplitude <= 0
                    ? 0
                    : (int) Math.min(100, Math.round((amplitudeSum / (double) sampleCount) / maxAmplitude * 100));
            points.add(value);
        }
        return points.isEmpty() ? null : List.copyOf(points);
    }

    private int decodeSignedSample(byte[] audioBytes, int offset, int sampleSizeInBytes, boolean bigEndian) {
        int sample = 0;
        if (bigEndian) {
            for (int index = 0; index < sampleSizeInBytes; index++) {
                sample = (sample << 8) | (audioBytes[offset + index] & 0xFF);
            }
        } else {
            for (int index = sampleSizeInBytes - 1; index >= 0; index--) {
                sample = (sample << 8) | (audioBytes[offset + index] & 0xFF);
            }
        }
        int shift = 32 - sampleSizeInBytes * 8;
        return (sample << shift) >> shift;
    }

    private StorageService resolveStorageService(FileInfo fileInfo) {
        if (fileInfo == null || !StrUtils.hasText(fileInfo.getStorageKey()) || !StrUtils.hasText(fileInfo.getFilePath())) {
            return null;
        }
        return storageManager.getStorageService(fileInfo.getStorageKey());
    }
}
