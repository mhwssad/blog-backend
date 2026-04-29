package com.cybzacg.blogbackend.module.chat.service.impl;

import com.cybzacg.blogbackend.common.storage.MediaAssetPathUtils;
import com.cybzacg.blogbackend.common.storage.StorageService;
import com.cybzacg.blogbackend.domain.FileInfo;
import com.cybzacg.blogbackend.module.chat.constant.ChatConstants;
import com.cybzacg.blogbackend.module.chat.model.common.ChatFilePayloadVO;
import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentMetadataResolver;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * 聊天语音附件处理器。<p>负责语音元数据补全（时长、波形）、WAV 预览生成和转码状态管理。</p>
 */
@Slf4j
@Component
class ChatAttachmentVoiceProcessor {

    boolean enrichVoicePayload(ChatFilePayloadVO payload, StorageService storageService, FileInfo fileInfo,
                               ChatAttachmentMetadataResolver metadataResolver) throws Exception {
        boolean changed = false;
        ChatAttachmentMetadataResolver.ChatAttachmentMetadata metadata =
                metadataResolver.resolve(fileInfo, ChatConstants.MESSAGE_TYPE_VOICE);
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
}
