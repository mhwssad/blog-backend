package com.cybzacg.blogbackend.module.content.comment.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "后台评论信息")
public class CommentVO {
    @Schema(description = "评论ID")
    private Long id;
    @Schema(description = "目标ID")
    private Long targetId;
    @Schema(description = "目标类型")
    private String targetType;
    @Schema(description = "评论内容")
    private String content;
    @Schema(description = "评论图片列表")
    private List<String> images;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户昵称")
    private String userNickname;
    @Schema(description = "用户头像")
    private String userAvatar;
    @Schema(description = "根评论ID")
    private Long rootId;
    @Schema(description = "父评论ID")
    private Long parentId;
    @Schema(description = "点赞数")
    private Integer likeCount;
    @Schema(description = "回复数")
    private Integer replyCount;
    @Schema(description = "评论状态")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;
    @Schema(description = "子评论列表")
    private List<CommentVO> children = new ArrayList<>();
}
