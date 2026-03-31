package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;

/**
 * 前台文章服务接口。
 *
 * <p>定义前台文章相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface PublicArticleService {
    /**
     * 分页查询当前用户可见的已发布文章列表，并按条件完成排序与过滤。
     */
    PageResult<PublicArticleCardVO> pageArticles(PublicArticlePageQuery query);

    /**
     * 查询前台文章详情，并在访问通过后补齐分类、标签和用户态信息。
     */
    PublicArticleDetailVO getArticle(Long id);
}
