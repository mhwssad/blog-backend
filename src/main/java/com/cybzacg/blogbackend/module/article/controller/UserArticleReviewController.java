package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.common.ArticleReviewLogVO;
import com.cybzacg.blogbackend.module.article.model.user.ArticleReviewSubmitRequest;
import com.cybzacg.blogbackend.module.article.service.UserArticleReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户文章审核控制器。
 */
@RestController
@RequestMapping("/api/user/articles")
@Tag(name = "用户文章审核")
@RequiredArgsConstructor
public class UserArticleReviewController {
    private final UserArticleReviewService userArticleReviewService;

    @PostMapping("/{id}/submit-review")
    @Operation(summary = "提交文章审核")
    public Result<Void> submitReview(@PathVariable Long id,
                                     @Valid @RequestBody(required = false) ArticleReviewSubmitRequest request) {
        userArticleReviewService.submitReview(id, request == null ? new ArticleReviewSubmitRequest() : request);
        return Result.success();
    }

    @GetMapping("/{id}/review-log")
    @Operation(summary = "查询文章审核日志")
    public Result<List<ArticleReviewLogVO>> listReviewLogs(@PathVariable Long id) {
        return Result.success(userArticleReviewService.listReviewLogs(id));
    }
}
