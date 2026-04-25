package com.cybzacg.blogbackend.module.article.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "前台文章卡片")
public class PublicArticleCardVO {
    @Schema(description = "文章ID")
    private Long id;
    @Schema(description = "文章标题")
    private String title;
    @Schema(description = "文章摘要")
    private String summary;
    @Schema(description = "封面图")
    private String coverImage;
    @Schema(description = "作者ID")
    private Long authorId;
    @Schema(description = "作者名称")
    private String authorName;
    @Schema(description = "是否置顶")
    private Integer isTop;
    @Schema(description = "访问级别")
    private Integer accessLevel;
    @Schema(description = "浏览数")
    private Integer viewCount;
    @Schema(description = "点赞数")
    private Integer likeCount;
    @Schema(description = "评论数")
    private Integer commentCount;
    @Schema(description = "收藏数")
    private Integer collectCount;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
