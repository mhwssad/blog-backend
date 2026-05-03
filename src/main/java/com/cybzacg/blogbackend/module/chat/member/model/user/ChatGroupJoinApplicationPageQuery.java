package com.cybzacg.blogbackend.module.chat.member.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入群申请分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "入群申请分页查询条件")
public class ChatGroupJoinApplicationPageQuery extends PageQuery {

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-已取消")
    private Integer applyStatus;
}
