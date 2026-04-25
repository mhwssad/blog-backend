package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.service.UserArticleActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户文章行为控制器。
 *
 * <p>负责对外暴露用户文章行为相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/user/articles")
@Tag(name = "用户文章行为")
@RequiredArgsConstructor
public class UserArticleActionController {
    private final UserArticleActionService userArticleActionService;

    @PostMapping("/{id}/likes")
    @Operation(summary = "点赞文章")
    public Result<Void> likeArticle(@PathVariable Long id) {
        userArticleActionService.likeArticle(id);
        return Result.success();
    }

    @DeleteMapping("/{id}/likes")
    @Operation(summary = "取消点赞文章")
    public Result<Void> unlikeArticle(@PathVariable Long id) {
        userArticleActionService.unlikeArticle(id);
        return Result.success();
    }
}
