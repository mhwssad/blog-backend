package com.cybzacg.blogbackend.dto.repository.article;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleSeriesItem;

import java.util.List;

/**
 * 文章系列关联 Repository。
 */
public interface BlogArticleSeriesItemRepository extends IService<BlogArticleSeriesItem> {

    /**
     * 按系列顺序读取系列文章关联。
     */
    List<BlogArticleSeriesItem> listBySeriesIdOrdered(Long seriesId);

    /**
     * 按文章读取所在系列关联。
     */
    List<BlogArticleSeriesItem> listByArticleId(Long articleId);

    /**
     * 判断系列内是否已存在文章。
     */
    boolean existsBySeriesIdAndArticleId(Long seriesId, Long articleId);

    /**
     * 读取系列当前最大顺序号。
     */
    Integer getMaxSeqNo(Long seriesId);

    /**
     * 删除系列下全部文章关联。
     */
    void removeBySeriesId(Long seriesId);

    /**
     * 删除指定系列中的文章关联。
     */
    void removeBySeriesIdAndArticleId(Long seriesId, Long articleId);
}
