package com.cybzacg.blogbackend.module.chat.conversation.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话列表分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会话列表分页查询条件")
public class ChatConversationPageQuery extends PageQuery {

    @Schema(description = "每页条数", example = "20")
    private Long size = 20L;

    @Schema(description = "关键字，当前支持按会话名和最后一条消息内容模糊查询")
    private String keyword;
}
