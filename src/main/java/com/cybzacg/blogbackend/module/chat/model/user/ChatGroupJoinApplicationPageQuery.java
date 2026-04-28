package com.cybzacg.blogbackend.module.chat.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 入群申请分页查询条件。
 */
@Data
@Schema(description = "入群申请分页查询条件")
public class ChatGroupJoinApplicationPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-已取消")
    private Integer applyStatus;
}
