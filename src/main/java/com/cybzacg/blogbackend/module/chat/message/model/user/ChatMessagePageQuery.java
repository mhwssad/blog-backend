package com.cybzacg.blogbackend.module.chat.message.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息历史分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息历史分页查询条件")
public class ChatMessagePageQuery extends PageQuery {

    @Schema(description = "每页条数", example = "20")
    private Long size = 20L;

    @Schema(description = "只查询该消息ID之前的历史消")
    private Long beforeMessageId;
}
