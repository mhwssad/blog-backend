package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleSeriesVO;
import com.cybzacg.blogbackend.module.article.service.ArticleSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台文章系列控制器。
 */
@RestController
@Tag(name = "前台文章系列")
@RequiredArgsConstructor
public class PublicArticleSeriesController {
    private final ArticleSeriesService articleSeriesService;

    @GetMapping("/api/public/authors/{authorId}/series")
    @Operation(summary = "查询作者公开系列列表")
    public Result<List<PublicArticleSeriesVO>> listAuthorSeries(@PathVariable Long authorId) {
        return Result.success(articleSeriesService.listAuthorSeries(authorId));
    }

    @GetMapping("/api/public/article-series/{id}")
    @Operation(summary = "查询公开系列详情")
    public Result<PublicArticleSeriesDetailVO> getSeries(@PathVariable Long id) {
        return Result.success(articleSeriesService.getPublicSeries(id));
    }
}
