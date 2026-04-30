package com.cybzacg.blogbackend.module.chat.conversation.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 群聊搜索分页条件。
 */
@Data
@Schema(description = "群聊搜索分页条件")
public class ChatGroupSearchQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 20L;

    @Schema(description = "关键词，按群名称和群简介搜索")
    private String keyword;

    @Schema(description = "群分类编码")
    private String categoryCode;
}
