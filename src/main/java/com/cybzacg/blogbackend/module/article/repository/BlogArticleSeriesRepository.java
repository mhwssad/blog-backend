package com.cybzacg.blogbackend.module.article.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.BlogArticleSeries;

import java.util.List;

/**
 * 文章系列 Repository。
 */
public interface BlogArticleSeriesRepository extends IService<BlogArticleSeries> {

    /**
     * 按创建人读取系列列表。
     */
    List<BlogArticleSeries> listByOwnerUserId(Long ownerUserId);

    /**
     * 统计指定创建人当前可公开展示的系列数量。
     */
    long countPublicVisibleByOwnerUserId(Long ownerUserId);
}
