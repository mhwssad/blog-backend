package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.module.article.model.internal.AuthorPublicProfileStats;

/**
 * 文章域对外资料查询服务。
 */
public interface ArticleProfileQueryService {

    /**
     * 查询作者公开主页所需的文章与系列统计。
     */
    AuthorPublicProfileStats getAuthorPublicProfileStats(Long authorId);
}
