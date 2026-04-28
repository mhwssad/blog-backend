package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesArticleRequest;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSaveRequest;
import com.cybzacg.blogbackend.module.article.model.user.ArticleSeriesSortRequest;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleSeriesVO;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户侧文章系列控制器。
 */
@RestController
@RequestMapping("/api/user/article-series")
@Tag(name = "用户文章系列")
@RequiredArgsConstructor
public class UserArticleSeriesController {
    private final ArticleSeriesService articleSeriesService;

    @GetMapping
    @Operation(summary = "查询我的系列列表")
    public Result<List<UserArticleSeriesVO>> listMySeries() {
        return Result.success(articleSeriesService.listMySeries());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询我的系列详情")
    public Result<UserArticleSeriesDetailVO> getMySeries(@PathVariable Long id) {
        return Result.success(articleSeriesService.getMySeries(id));
    }

    @PostMapping
    @Operation(summary = "创建系列")
    public Result<UserArticleSeriesDetailVO> createSeries(@Valid @RequestBody ArticleSeriesSaveRequest request) {
        return Result.success(articleSeriesService.createSeries(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改系列")
    public Result<UserArticleSeriesDetailVO> updateSeries(@PathVariable Long id,
                                                          @Valid @RequestBody ArticleSeriesSaveRequest request) {
        return Result.success(articleSeriesService.updateSeries(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除系列")
    public Result<Void> deleteSeries(@PathVariable Long id) {
        articleSeriesService.deleteSeries(id);
        return Result.success();
    }

    @PostMapping("/{id}/articles")
    @Operation(summary = "向系列加入文章")
    public Result<UserArticleSeriesDetailVO> addArticle(@PathVariable Long id,
                                                        @Valid @RequestBody ArticleSeriesArticleRequest request) {
        return Result.success(articleSeriesService.addArticle(id, request));
    }

    @DeleteMapping("/{id}/articles/{articleId}")
    @Operation(summary = "从系列移出文章")
    public Result<UserArticleSeriesDetailVO> removeArticle(@PathVariable Long id,
                                                           @PathVariable Long articleId) {
        return Result.success(articleSeriesService.removeArticle(id, articleId));
    }

    @PutMapping("/{id}/articles/sort")
    @Operation(summary = "调整系列文章顺序")
    public Result<UserArticleSeriesDetailVO> sortArticles(@PathVariable Long id,
                                                          @Valid @RequestBody ArticleSeriesSortRequest request) {
        return Result.success(articleSeriesService.sortArticles(id, request));
    }
}
