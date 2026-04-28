package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.admin.*;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 文章后台管理控制器。
 *
 * <p>负责对外暴露文章后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/articles")
@Tag(name = "后台文章管理")
@RequiredArgsConstructor
public class ArticleAdminController {
    private final ArticleAdminService articleAdminService;

    @GetMapping
    @Operation(summary = "分页查询文章")
    @PreAuthorize("@permission.hasPermission('content:article:query')")
    public Result<PageResult<ArticleAdminVO>> pageArticles(ArticleAdminPageQuery query) {
        return Result.success(articleAdminService.pageArticles(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文章详情")
    @PreAuthorize("@permission.hasPermission('content:article:query')")
    public Result<ArticleDetailVO> getArticle(@PathVariable Long id) {
        return Result.success(articleAdminService.getArticle(id));
    }

    @PostMapping
    @Operation(summary = "新增文章")
    @PreAuthorize("@permission.hasPermission('content:article:create')")
    public Result<ArticleDetailVO> createArticle(@Valid @RequestBody ArticleSaveRequest request) {
        return Result.success(articleAdminService.createArticle(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改文章")
    @PreAuthorize("@permission.hasPermission('content:article:update')")
    public Result<ArticleDetailVO> updateArticle(@PathVariable Long id,
                                                 @Valid @RequestBody ArticleSaveRequest request) {
        return Result.success(articleAdminService.updateArticle(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改文章状态")
    @PreAuthorize("@permission.hasPermission('content:article:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody ArticleStatusRequest request) {
        articleAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/access")
    @Operation(summary = "配置文章访问权限")
    @PreAuthorize("@permission.hasPermission('content:article:access')")
    public Result<Void> assignAccess(@PathVariable Long id,
                                     @Valid @RequestBody ArticleAccessAssignRequest request) {
        articleAdminService.assignAccess(id, request.getAccessList());
        return Result.success();
    }

    @PutMapping("/{id}/top")
    @Operation(summary = "切换文章置顶状态")
    @PreAuthorize("@permission.hasPermission('content:article:update')")
    public Result<Void> toggleTop(@PathVariable Long id,
                                  @RequestParam boolean enabled) {
        articleAdminService.toggleTop(id, enabled);
        return Result.success();
    }

    @PutMapping("/{id}/recommend")
    @Operation(summary = "切换文章推荐状态")
    @PreAuthorize("@permission.hasPermission('content:article:update')")
    public Result<Void> toggleRecommend(@PathVariable Long id,
                                        @RequestParam boolean enabled) {
        articleAdminService.toggleRecommend(id, enabled);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    @PreAuthorize("@permission.hasPermission('content:article:delete')")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleAdminService.deleteArticle(id);
        return Result.success();
    }
}
