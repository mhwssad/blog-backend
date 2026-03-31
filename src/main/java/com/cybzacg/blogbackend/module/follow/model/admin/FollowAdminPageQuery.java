package com.cybzacg.blogbackend.module.follow.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 关注关系后台分页查询条件。
 */
@Data
@Schema(description = "关注关系后台分页查询条件")
public class FollowAdminPageQuery {
    @Schema(description = "页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;

    @Schema(description = "关注者用户ID")
    private Long followerId;

    @Schema(description = "被关注者用户ID")
    private Long followingId;

    @Schema(description = "关注状态：0-已取关，1-已关注")
    private Integer followStatus;

    @Schema(description = "是否特别关注：0-否，1-是")
    private Integer specialFollow;

    @Schema(description = "关注来源")
    private String source;

    @Schema(description = "关键词，匹配关注双方用户名或昵称")
    private String keyword;
}
