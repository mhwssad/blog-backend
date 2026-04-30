package com.cybzacg.blogbackend.module.chat.message.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 消息历史分页查询条件。
 */
@Data
@Schema(description = "消息历史分页查询条件")
public class ChatMessagePageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "20")
    private Long size = 20L;

    @Schema(description = "只查询该消息ID之前的历史消息")
    private Long beforeMessageId;
}
