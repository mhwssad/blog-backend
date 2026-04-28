package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 群邀请链接分页查询条件。
 */
@Data
@Schema(description = "群邀请链接分页查询条件")
public class ChatGroupInviteLinkPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
