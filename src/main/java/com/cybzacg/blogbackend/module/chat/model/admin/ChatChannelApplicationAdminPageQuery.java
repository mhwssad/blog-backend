package com.cybzacg.blogbackend.module.chat.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台频道创建申请分页查询条件。
 */
@Data
@Schema(description = "后台频道创建申请分页查询条件")
public class ChatChannelApplicationAdminPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充")
    private Integer applyStatus;

    @Schema(description = "申请用户ID")
    private Long applicantUserId;

    @Schema(description = "频道名称关键字")
    private String keyword;
}
