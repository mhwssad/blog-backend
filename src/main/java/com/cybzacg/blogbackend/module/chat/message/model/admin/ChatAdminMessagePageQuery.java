package com.cybzacg.blogbackend.module.chat.message.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台消息分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台消息分页查询条件")
public class ChatAdminMessagePageQuery extends PageQuery {

    @Schema(description = "每页条数")
    private Long size = 20L;

    @Schema(description = "仅查询该消息ID之前的历史消�")
    private Long beforeMessageId;

    @Schema(description = "发送人ID")
    private Long senderId;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "关键字，当前支持按消息内容筛")
    private String keyword;
}
