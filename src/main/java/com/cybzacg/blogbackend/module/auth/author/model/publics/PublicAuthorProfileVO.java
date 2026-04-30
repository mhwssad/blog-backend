package com.cybzacg.blogbackend.module.auth.author.model.publics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 公开作者主页摘要视图。
 */
@Data
@Schema(description = "公开作者主页摘要")
public class PublicAuthorProfileVO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "用户等级")
    private Integer userLevel;

    @Schema(description = "是否作者")
    private Boolean author;

    @Schema(description = "作者身份标识，当前作者固定返回 author")
    private String authorBadge;

    @Schema(description = "公开文章数")
    private Long publicArticleCount;

    @Schema(description = "公开系列数")
    private Long publicSeriesCount;

    @Schema(description = "作品展示位文章ID列表，当前阶段预留")
    private List<Long> showcaseArticleIds;

    @Schema(description = "代表内容文章ID列表，当前阶段预留")
    private List<Long> representativeArticleIds;

    @Schema(description = "系列展示位系列ID列表，当前阶段预留")
    private List<Long> featuredSeriesIds;

    @Schema(description = "专栏展示位ID列表，当前阶段预留")
    private List<Long> featuredColumnIds;
}
