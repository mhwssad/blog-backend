package com.cybzacg.blogbackend.module.forum.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台论坛帖子详情 VO，在列表 VO 基础上增加帖子内容。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台论坛帖子详情VO")
public class ForumPostAdminDetailVO extends ForumPostAdminVO {
    @Schema(description = "帖子内容")
    private String content;
}
