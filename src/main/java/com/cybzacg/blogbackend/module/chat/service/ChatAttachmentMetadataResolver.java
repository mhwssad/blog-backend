package com.cybzacg.blogbackend.module.chat.service;

import com.cybzacg.blogbackend.domain.FileInfo;

import java.util.List;

/**
 * 聊天附件元数据解析器。
 *
 * <p>负责在发送图片/语音消息时，尽力补齐宽高、时长和波形等展示所需元数据。
 */
public interface ChatAttachmentMetadataResolver {
    ChatAttachmentMetadata resolve(FileInfo fileInfo, String messageType);

    /**
     * 附件元数据解析结果。
     */
    record ChatAttachmentMetadata(Integer width,
                                  Integer height,
                                  Integer durationSeconds,
                                  List<Integer> waveform) {
        private static final ChatAttachmentMetadata EMPTY = new ChatAttachmentMetadata(null, null, null, null);

        public static ChatAttachmentMetadata empty() {
            return EMPTY;
        }
    }
}
