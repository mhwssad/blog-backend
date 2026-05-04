package com.cybzacg.blogbackend.module.forum.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "公开论坛帖子列表项")
public class PublicForumPostVO {
    @Schema(description = "帖子ID")
    private Long id;
    @Schema(description = "版块ID")
    private Long sectionId;
    @Schema(description = "版块名称")
    private String sectionName;
    @Schema(description = "作者ID")
    private Long authorId;
    @Schema(description = "作者名称")
    private String authorName;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "可见范围")
    private Integer visibilityScope;
    @Schema(description = "是否置顶")
    private Integer isTop;
    @Schema(description = "是否精华")
    private Integer isEssence;
    @Schema(description = "浏览数")
    private Integer viewCount;
    @Schema(description = "点赞数")
    private Integer likeCount;
    @Schema(description = "回复数")
    private Integer replyCount;
    @Schema(description = "收藏数")
    private Integer collectCount;
    @Schema(description = "分享数")
    private Integer shareCount;
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
