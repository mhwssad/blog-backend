package com.cybzacg.blogbackend.module.forum.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台论坛回复列表展示 VO。
 */
@Data
@Schema(description = "后台论坛回复列表VO")
public class ForumReplyAdminVO {
    @Schema(description = "回复ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "帖子标题")
    private String postTitle;

    @Schema(description = "父回复ID，顶级回复为0")
    private Long parentId;

    @Schema(description = "根回复ID，顶级回复为0")
    private Long rootId;

    @Schema(description = "回复用户ID")
    private Long userId;

    @Schema(description = "回复用户昵称")
    private String userName;

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "状态值")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusName;

    @Schema(description = "楼层号")
    private Integer floorNo;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
