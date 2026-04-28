package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.module.article.model.common.ArticleReviewLogVO;
import com.cybzacg.blogbackend.module.article.model.user.ArticleReviewSubmitRequest;

import java.util.List;

/**
 * 用户文章审核服务。
 */
public interface UserArticleReviewService {

    /**
     * 提交或重新提交文章审核。
     */
    void submitReview(Long articleId, ArticleReviewSubmitRequest request);

    /**
     * 查询当前用户文章的审核日志。
     */
    List<ArticleReviewLogVO> listReviewLogs(Long articleId);
}
