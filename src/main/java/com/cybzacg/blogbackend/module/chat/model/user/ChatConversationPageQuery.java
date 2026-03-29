package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话列表分页查询条件。
 */
@Data
@Schema(description = "会话列表分页查询条件")
public class ChatConversationPageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "20")
    private Long size = 20L;

    @Schema(description = "关键字，当前支持按会话名和最后一条消息内容模糊过滤")
    private String keyword;
}
