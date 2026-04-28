package com.cybzacg.blogbackend.module.article.model.publics;

import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesSummaryVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.model.publics.PublicTagVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "前台文章详情")
public class PublicArticleDetailVO {
    @Schema(description = "文章ID")
    private Long id;
    @Schema(description = "文章标题")
    private String title;
    @Schema(description = "文章摘要")
    private String summary;
    @Schema(description = "文章内容")
    private String content;
    @Schema(description = "封面图")
    private String coverImage;
    @Schema(description = "作者ID")
    private Long authorId;
    @Schema(description = "作者名称")
    private String authorName;
    @Schema(description = "是否置顶")
    private Integer isTop;
    @Schema(description = "是否推荐")
    private Integer isRecommend;
    @Schema(description = "是否原创")
    private Integer isOriginal;
    @Schema(description = "来源地址")
    private String sourceUrl;
    @Schema(description = "访问级别")
    private Integer accessLevel;
    @Schema(description = "可见范围")
    private Integer visibilityScope;
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
    private LocalDateTime publishTime;
    @Schema(description = "分类列表")
    private List<PublicCategoryTreeVO> categories;
    @Schema(description = "标签列表")
    private List<PublicTagVO> tags;
    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;
    @Schema(description = "当前用户是否已收藏")
    private Boolean collected;
    @Schema(description = "当前用户是否允许评论")
    private Boolean canComment;
    @Schema(description = "所属系列摘要")
    private List<ArticleSeriesSummaryVO> seriesList;
}
