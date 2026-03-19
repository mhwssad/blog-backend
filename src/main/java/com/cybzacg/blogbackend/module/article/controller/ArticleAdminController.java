package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessAssignRequest;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminPageQuery;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleSaveRequest;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleStatusRequest;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    @PreAuthorize("@permission.hasPermission('content:article:delete')")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleAdminService.deleteArticle(id);
        return Result.success();
    }
}
