package com.cybzacg.blogbackend.module.chat.message.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台消息回执分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台消息回执分页查询条件")
public class ChatAdminMessageReceiptPageQuery extends PageQuery {

    @Schema(description = "每页条数")
    private Long size = 20L;

    @Schema(description = "接收人用户ID")
    private Long recipientUserId;

    @Schema(description = "投递状态：0-待投递，1-已送达�?-已读")
    private Integer deliveryStatus;

    @Schema(description = "可见状态：0-已隐藏，1-可见")
    private Integer visibleStatus;
}
