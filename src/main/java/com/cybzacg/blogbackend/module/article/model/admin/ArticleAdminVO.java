package com.cybzacg.blogbackend.module.article.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "后台文章信息")
public class ArticleAdminVO {
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
    @Schema(description = "是否原创")
    private Integer isOriginal;
    @Schema(description = "文章状态")
    private Integer status;
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
    @Schema(description = "分享数")
    private Integer shareCount;
    @Schema(description = "发布时间")
    private Date publishTime;
    @Schema(description = "创建时间")
    private Date createdAt;
    @Schema(description = "更新时间")
    private Date updatedAt;
    @Schema(description = "备注")
    private String remark;
}
