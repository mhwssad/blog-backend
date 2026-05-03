package com.cybzacg.blogbackend.module.chat.member.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台频道创建申请分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台频道创建申请分页查询条件")
public class ChatChannelApplicationAdminPageQuery extends PageQuery {

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-待补")
    private Integer applyStatus;

    @Schema(description = "申请用户ID")
    private Long applicantUserId;

    @Schema(description = "频道名称关键字")
    private String keyword;
}
