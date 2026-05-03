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
public interface ArticleModelConvert {
    ArticleAdminVO toAdminVO(BlogArticle article);

    ArticleDetailVO toDetailVO(BlogArticle article);

    UserArticleVO toUserVO(BlogArticle article);

    UserArticleDetailVO toUserDetailVO(BlogArticle article);

    PublicArticleCardVO toPublicCardVO(BlogArticle article);

    PublicArticleDetailVO toPublicDetailVO(BlogArticle article);

    ArticleAccessItem toAccessItem(BlogArticleAccess access);

    @Mapping(target = "title", expression = "java(StrUtils.normalize(request.getTitle()))")
    @Mapping(target = "summary", expression = "java(StrUtils.normalize(request.getSummary()))")
    @Mapping(target = "coverImage", expression = "java(StrUtils.normalize(request.getCoverImage()))")
    @Mapping(target = "sourceUrl", expression = "java(StrUtils.normalize(request.getSourceUrl()))")
    @Mapping(target = "remark", expression = "java(StrUtils.normalize(request.getRemark()))")
    BlogArticle toArticle(ArticleSaveRequest request);

    @InheritConfiguration(name = "toArticle")
    void updateArticle(ArticleSaveRequest request, @MappingTarget BlogArticle article);

    BlogArticleCategory toArticleCategory(Long articleId, Long categoryId, Integer sortOrder);

    SysTagRelation toTagRelation(Long tagId, Long targetId, String targetType);

    @Mapping(target = "userId", source = "item.userId")
    @Mapping(target = "accessType", source = "item.accessType")
    @Mapping(target = "expireTime", source = "item.expireTime")
    @Mapping(target = "grantReason", expression = "java(StrUtils.normalize(item.getGrantReason()))")
    BlogArticleAccess toArticleAccess(Long articleId, ArticleAccessItem item, LocalDateTime grantTime);
}
