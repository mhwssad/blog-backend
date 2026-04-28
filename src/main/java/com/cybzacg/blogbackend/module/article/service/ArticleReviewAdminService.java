package com.cybzacg.blogbackend.module.article.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewAdminDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewDecisionRequest;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleReviewRepairRequest;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;

/**
 * 文章审核后台服务。
 */
public interface ArticleReviewAdminService {

    /**
     * 分页查询文章审核记录。
     */
    PageResult<ArticleAdminVO> pageReviews(ArticleReviewAdminPageQuery query);

    /**
     * 查询文章审核详情。
     */
    ArticleReviewAdminDetailVO getReviewDetail(Long articleId);

    /**
     * 审核通过文章。
     */
    void approveReview(Long articleId, ArticleReviewDecisionRequest request);

    /**
     * 审核拒绝文章。
     */
    void rejectReview(Long articleId, ArticleReviewDecisionRequest request);

    /**
     * 修正文章审核状态。
     */
    void repairReviewStatus(Long articleId, ArticleReviewRepairRequest request);
}
