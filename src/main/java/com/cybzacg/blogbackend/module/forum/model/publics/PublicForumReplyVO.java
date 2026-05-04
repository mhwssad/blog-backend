package com.cybzacg.blogbackend.module.forum.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "公开论坛回复")
public class PublicForumReplyVO {
    @Schema(description = "回复ID")
    private Long id;
    @Schema(description = "帖子ID")
    private Long postId;
    @Schema(description = "父回复ID")
    private Long parentId;
    @Schema(description = "根回复ID")
    private Long rootId;
    @Schema(description = "回复用户ID")
    private Long userId;
    @Schema(description = "回复用户名称")
    private String userName;
    @Schema(description = "回复内容")
    private String content;
    @Schema(description = "状态")
    private Integer status;
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
    @Schema(description = "子回复")
    private List<PublicForumReplyVO> children = new ArrayList<>();
}
