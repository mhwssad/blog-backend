package com.cybzacg.blogbackend.module.auth.author.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作者申请后台分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "作者申请后台分页查询条件")
public class SysAuthorApplicationAdminPageQuery extends PageQuery {

    @Schema(description = "申请用户ID")
    private Long userId;

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-待补")
    private Integer applyStatus;

    @Schema(description = "关键词，匹配申请说明、内容方向和个人简介")
    private String keyword;
}
