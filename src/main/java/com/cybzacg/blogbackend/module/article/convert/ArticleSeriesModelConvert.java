package com.cybzacg.blogbackend.module.article.convert;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleSeries;
import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesArticleVO;
import com.cybzacg.blogbackend.module.article.model.common.ArticleSeriesSummaryVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesVO;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSaveRequest;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesVO;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

/**
 * 文章系列对象转换器。
 */
@Mapper(
        componentModel = "spring",
        imports = StrUtils.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ArticleSeriesModelConvert {

    /**
     * 将系列保存请求转换为系列实体。
     */
    @Mapping(target = "title", expression = "java(StrUtils.normalize(request.getTitle()))")
    @Mapping(target = "description", expression = "java(StrUtils.normalize(request.getDescription()))")
    @Mapping(target = "coverImage", expression = "java(StrUtils.normalize(request.getCoverImage()))")
    BlogArticleSeries toSeries(ArticleSeriesSaveRequest request);

    /**
     * 将系列保存请求更新到现有系列实体。
     */
    @InheritConfiguration(name = "toSeries")
    void updateSeries(ArticleSeriesSaveRequest request, @MappingTarget BlogArticleSeries series);

    /**
     * 系列实体转用户系列摘要。
     */
    UserArticleSeriesVO toUserSeriesVO(BlogArticleSeries series);

    /**
     * 系列实体转用户系列详情。
     */
    UserArticleSeriesDetailVO toUserSeriesDetailVO(BlogArticleSeries series);

    /**
     * 系列实体转公开系列摘要。
     */
    PublicArticleSeriesVO toPublicSeriesVO(BlogArticleSeries series);

    /**
     * 系列实体转公开系列详情。
     */
    PublicArticleSeriesDetailVO toPublicSeriesDetailVO(BlogArticleSeries series);

    /**
     * 系列实体转文章详情中的系列摘要。
     */
    ArticleSeriesSummaryVO toSeriesSummaryVO(BlogArticleSeries series);

    /**
     * 文章实体转系列内文章项。
     */
    ArticleSeriesArticleVO toSeriesArticleVO(BlogArticle article);
}
