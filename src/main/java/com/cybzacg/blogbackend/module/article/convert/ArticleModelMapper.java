package com.cybzacg.blogbackend.module.article.convert;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.utils.StrUtils;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Date;

@Mapper(componentModel = "spring", imports = StrUtils.class)
public interface ArticleModelMapper {
    ArticleAdminVO toAdminVO(BlogArticle article);

    ArticleDetailVO toDetailVO(BlogArticle article);

    PublicArticleCardVO toPublicCardVO(BlogArticle article);

    PublicArticleDetailVO toPublicDetailVO(BlogArticle article);

    ArticleAccessItem toAccessItem(BlogArticleAccess access);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", expression = "java(StrUtils.normalize(request.getTitle()))")
    @Mapping(target = "summary", expression = "java(StrUtils.normalize(request.getSummary()))")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "coverImage", expression = "java(StrUtils.normalize(request.getCoverImage()))")
    @Mapping(target = "authorId", source = "authorId")
    @Mapping(target = "isTop", source = "isTop")
    @Mapping(target = "isOriginal", source = "isOriginal")
    @Mapping(target = "sourceUrl", expression = "java(StrUtils.normalize(request.getSourceUrl()))")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "publishTime", source = "publishTime")
    @Mapping(target = "accessLevel", source = "accessLevel")
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
    BlogArticleAccess toArticleAccess(Long articleId, ArticleAccessItem item, Date grantTime);
}
