package com.cybzacg.blogbackend.module.chat.member.model.user;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群邀请链接分页查询条件�? */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "群邀请链接分页查询条")
public class ChatGroupInviteLinkPageQuery extends PageQuery {

    @Schema(description = "状态：0-停用�?-启用")
    private Integer status;
}
