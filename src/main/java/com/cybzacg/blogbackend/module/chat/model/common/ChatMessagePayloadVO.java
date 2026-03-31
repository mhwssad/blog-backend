package com.cybzacg.blogbackend.module.chat.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 聊天消息扩展载荷。
 */
@Data
@Schema(description = "聊天消息扩展载荷")
public class ChatMessagePayloadVO {
    @Schema(description = "附件载荷")
    private ChatFilePayloadVO file;

    @Schema(description = "回复消息快照")
    private ChatReplyMessageVO reply;
}
