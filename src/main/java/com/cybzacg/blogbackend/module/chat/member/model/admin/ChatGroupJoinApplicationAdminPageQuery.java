package com.cybzacg.blogbackend.module.chat.member.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台群入群申请分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台群入群申请分页查询条件")
public class ChatGroupJoinApplicationAdminPageQuery extends PageQuery {

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-已取消")
    private Integer applyStatus;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "申请用户ID")
    private Long applicantUserId;

    @Schema(description = "申请附言或审核意见关键字")
    private String keyword;
}
