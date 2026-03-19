package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 前台文章服务接口。
 *
 * <p>定义前台文章相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface PublicArticleService {
    PageResult<PublicArticleCardVO> pageArticles(PublicArticlePageQuery query);

    PublicArticleDetailVO getArticle(Long id, HttpServletRequest request);
}
