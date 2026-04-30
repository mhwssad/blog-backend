package com.cybzacg.blogbackend.module.chat.member.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户频道创建申请分页查询条件。
 */
@Data
@Schema(description = "用户频道创建申请分页查询条件")
public class ChatChannelApplicationPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;
}
