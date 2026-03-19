package com.cybzacg.blogbackend.module.article.convert;

import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.BlogArticleAccess;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessItem;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleModelMapper {
    ArticleAdminVO toAdminVO(BlogArticle article);

    ArticleDetailVO toDetailVO(BlogArticle article);

    PublicArticleCardVO toPublicCardVO(BlogArticle article);

    PublicArticleDetailVO toPublicDetailVO(BlogArticle article);

    ArticleAccessItem toAccessItem(BlogArticleAccess access);
}
