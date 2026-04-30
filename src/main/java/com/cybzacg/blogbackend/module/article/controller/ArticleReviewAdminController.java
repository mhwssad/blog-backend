package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.admin.*;
import com.cybzacg.blogbackend.module.article.service.ArticleReviewAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台文章审核控制器。
 */
@RestController
@RequestMapping("/api/sys/article-reviews")
@Tag(name = "后台文章审核")
@RequiredArgsConstructor
public class ArticleReviewAdminController {
    private final ArticleReviewAdminService articleReviewAdminService;

    @GetMapping
    @Operation(summary = "分页查询文章审核")
    @PreAuthorize("@permission.hasPermission('content:article-review:query')")
    public Result<PageResult<ArticleAdminVO>> pageReviews(ArticleReviewAdminPageQuery query) {
        return Result.success(articleReviewAdminService.pageReviews(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文章审核详情")
    @PreAuthorize("@permission.hasPermission('content:article-review:query')")
    public Result<ArticleReviewAdminDetailVO> getReviewDetail(@PathVariable Long id) {
        return Result.success(articleReviewAdminService.getReviewDetail(id));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过文章")
    @PreAuthorize("@permission.hasPermission('content:article-review:review')")
    public Result<Void> approveReview(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) ArticleReviewDecisionRequest request) {
        articleReviewAdminService.approveReview(id, request == null ? new ArticleReviewDecisionRequest() : request);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "审核拒绝文章")
    @PreAuthorize("@permission.hasPermission('content:article-review:review')")
    public Result<Void> rejectReview(@PathVariable Long id,
                                     @Valid @RequestBody ArticleReviewDecisionRequest request) {
        articleReviewAdminService.rejectReview(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/repair-status")
    @Operation(summary = "修正文章审核状态")
    @PreAuthorize("@permission.hasPermission('content:article-review:repair')")
    public Result<Void> repairReviewStatus(@PathVariable Long id,
                                           @Valid @RequestBody ArticleReviewRepairRequest request) {
        articleReviewAdminService.repairReviewStatus(id, request);
        return Result.success();
    }
}
