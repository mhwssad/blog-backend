package com.cybzacg.blogbackend.dto.repository.article;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.article.BlogArticleReviewLog;

import java.util.List;

/**
 * 文章审核日志 Repository。
 */
public interface BlogArticleReviewLogRepository extends IService<BlogArticleReviewLog> {

    /**
     * 按文章读取审核日志，按操作时间倒序返回。
     */
    List<BlogArticleReviewLog> listByArticleId(Long articleId);
}
