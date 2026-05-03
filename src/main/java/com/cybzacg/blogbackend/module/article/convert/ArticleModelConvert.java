package com.cybzacg.blogbackend.module.article.convert;

import com.cybzacg.blogbackend.domain.article.BlogArticle;
import com.cybzacg.blogbackend.domain.article.BlogArticleAccess;
import com.cybzacg.blogbackend.domain.article.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.content.SysTagRelation;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleVO;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.*;

import java.time.LocalDateTime;

/**
 * 文章模块对象转换器，处理文章、分类关联、标签关联及访问权限的映射。
 */
@Mapper(
        componentModel = "spring",
        imports = StrUtils.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ArticleModelMapper {
    @Mapping(target = "reviewStatus", source = "reviewStatus")
    @Mapping(target = "scheduledPublishTime", source = "scheduledPublishTime")
    @Mapping(target = "visibilityScope", source = "visibilityScope")
    ArticleAdminVO toAdminVO(BlogArticle article);

    @Mapping(target = "reviewStatus", source = "reviewStatus")
    @Mapping(target = "scheduledPublishTime", source = "scheduledPublishTime")
    @Mapping(target = "visibilityScope", source = "visibilityScope")
    ArticleDetailVO toDetailVO(BlogArticle article);

    @Mapping(target = "reviewStatus", source = "reviewStatus")
    @Mapping(target = "scheduledPublishTime", source = "scheduledPublishTime")
    @Mapping(target = "visibilityScope", source = "visibilityScope")
    UserArticleVO toUserVO(BlogArticle article);

    @Mapping(target = "reviewStatus", source = "reviewStatus")
    @Mapping(target = "scheduledPublishTime", source = "scheduledPublishTime")
    @Mapping(target = "visibilityScope", source = "visibilityScope")
    UserArticleDetailVO toUserDetailVO(BlogArticle article);

    PublicArticleCardVO toPublicCardVO(BlogArticle article);

    @Mapping(target = "visibilityScope", source = "visibilityScope")
    PublicArticleDetailVO toPublicDetailVO(BlogArticle article);

    ArticleAccessItem toAccessItem(BlogArticleAccess access);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", expression = "java(StrUtils.normalize(request.getTitle()))")
    @Mapping(target = "summary", expression = "java(StrUtils.normalize(request.getSummary()))")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "coverImage", expression = "java(StrUtils.normalize(request.getCoverImage()))")
    @Mapping(target = "authorId", source = "authorId")
    @Mapping(target = "isTop", source = "isTop")
    @Mapping(target = "isRecommend", source = "isRecommend")
    @Mapping(target = "isOriginal", source = "isOriginal")
    @Mapping(target = "sourceUrl", expression = "java(StrUtils.normalize(request.getSourceUrl()))")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "publishTime", source = "publishTime")
    @Mapping(target = "scheduledPublishTime", source = "scheduledPublishTime")
    @Mapping(target = "accessLevel", source = "accessLevel")
    @Mapping(target = "visibilityScope", source = "visibilityScope")
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "collectCount", ignore = true)
    @Mapping(target = "shareCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "remark", expression = "java(StrUtils.normalize(request.getRemark()))")
    BlogArticle toArticle(ArticleSaveRequest request);

    @InheritConfiguration(name = "toArticle")
    void updateArticle(ArticleSaveRequest request, @MappingTarget BlogArticle article);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "articleId", source = "articleId")
    @Mapping(target = "categoryId", source = "categoryId")
    @Mapping(target = "sortOrder", source = "sortOrder")
    @Mapping(target = "createdAt", ignore = true)
    BlogArticleCategory toArticleCategory(Long articleId, Long categoryId, Integer sortOrder);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tagId", source = "tagId")
    @Mapping(target = "targetId", source = "targetId")
    @Mapping(target = "targetType", source = "targetType")
    @Mapping(target = "createdAt", ignore = true)
    SysTagRelation toTagRelation(Long tagId, Long targetId, String targetType);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "articleId", source = "articleId")
    @Mapping(target = "userId", source = "item.userId")
    @Mapping(target = "accessType", source = "item.accessType")
    @Mapping(target = "grantTime", source = "grantTime")
    @Mapping(target = "expireTime", source = "item.expireTime")
    @Mapping(target = "grantReason", expression = "java(StrUtils.normalize(item.getGrantReason()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BlogArticleAccess toArticleAccess(Long articleId, ArticleAccessItem item, LocalDateTime grantTime);
}
