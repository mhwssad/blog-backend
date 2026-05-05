package com.cybzacg.blogbackend.module.forum.model.admin;

import com.cybzacg.blogbackend.core.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台论坛回复分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台论坛回复分页查询参数")
public class ForumReplyAdminPageQuery extends PageQuery {
    @Schema(description = "回复内容关键字")
    private String keyword;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "回复用户ID")
    private Long userId;

    @Schema(description = "状态：1-正常，2-隐藏，3-已删除，4-审核中")
    private Integer status;
}
