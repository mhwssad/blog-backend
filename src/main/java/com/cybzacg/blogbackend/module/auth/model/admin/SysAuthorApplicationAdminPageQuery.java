package com.cybzacg.blogbackend.module.auth.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作者申请后台分页查询条件。
 */
@Data
@Schema(description = "作者申请后台分页查询条件")
public class SysAuthorApplicationAdminPageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    @Schema(description = "申请用户ID")
    private Long userId;

    @Schema(description = "申请状态：0-待审核，1-已通过，2-已拒绝，3-待补充")
    private Integer applyStatus;

    @Schema(description = "关键词，匹配申请说明、内容方向和个人简介")
    private String keyword;
}
