package com.cybzacg.blogbackend.module.follow.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关注关系后台分页查询条件�? */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "关注关系后台分页查询条件")
public class FollowAdminPageQuery extends PageQuery {

    @Schema(description = "关注者用户ID")
    private Long followerId;

    @Schema(description = "被关注者用户ID")
    private Long followingId;

    @Min(value = 0, message = "关注状态不合法")
    @Max(value = 1, message = "关注状态不合法")
    @Schema(description = "关注状态：0-已取关，1-已关注")
    private Integer followStatus;

    @Min(value = 0, message = "特别关注状态不合法")
    @Max(value = 1, message = "特别关注状态不合法")
    @Schema(description = "是否特别关注：0-否，1-是")
    private Integer specialFollow;

    @Schema(description = "关注来源")
    private String source;

    @Schema(description = "关键词，匹配关注双方用户名或昵称")
    private String keyword;
}
