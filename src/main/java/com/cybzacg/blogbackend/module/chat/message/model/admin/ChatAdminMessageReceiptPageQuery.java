package com.cybzacg.blogbackend.module.chat.message.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台消息回执分页查询条件。
 */
@Data
@Schema(description = "后台消息回执分页查询条件")
public class ChatAdminMessageReceiptPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 20L;

    @Schema(description = "接收人用户ID")
    private Long recipientUserId;

    @Schema(description = "投递状态：0-待投递，1-已送达，2-已读")
    private Integer deliveryStatus;

    @Schema(description = "可见状态：0-已隐藏，1-可见")
    private Integer visibleStatus;
}
