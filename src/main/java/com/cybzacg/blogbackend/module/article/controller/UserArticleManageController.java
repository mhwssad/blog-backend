package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAccessAssignRequest;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.user.UserArticlePageQuery;
import com.cybzacg.blogbackend.module.article.model.user.UserArticleVO;
import com.cybzacg.blogbackend.module.article.service.UserArticleManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧文章管理控制器。
 */
@RestController
@RequestMapping("/api/user/articles")
@Tag(name = "用户文章管理")
@RequiredArgsConstructor
public class UserArticleManageController {
    private final UserArticleManageService userArticleManageService;

    @GetMapping
    @Operation(summary = "分页查询我的文章")
    public Result<PageResult<UserArticleVO>> pageMyArticles(UserArticlePageQuery query) {
        return Result.success(userArticleManageService.pageMyArticles(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询我的文章详情")
    public Result<UserArticleDetailVO> getMyArticle(@PathVariable Long id) {
        return Result.success(userArticleManageService.getMyArticle(id));
    }

    @PutMapping("/{id}/access")
    @Operation(summary = "配置我的文章访问名单")
    public Result<Void> assignMyArticleAccess(@PathVariable Long id,
                                              @Valid @RequestBody ArticleAccessAssignRequest request) {
        userArticleManageService.assignMyArticleAccess(id, request.getAccessList());
        return Result.success();
    }
}
