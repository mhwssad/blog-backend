package com.cybzacg.blogbackend.module.article.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleDetailVO;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticlePageQuery;
import com.cybzacg.blogbackend.module.article.service.PublicArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台文章控制器。
 *
 * <p>负责对外暴露前台文章相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/articles")
@Tag(name = "前台文章接口")
@RequiredArgsConstructor
public class PublicArticleController {
    private final PublicArticleService publicArticleService;

    @GetMapping
    @Operation(summary = "分页查询已发布文章")
    public Result<PageResult<PublicArticleCardVO>> pageArticles(PublicArticlePageQuery query) {
        return Result.success(publicArticleService.pageArticles(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文章详情")
    public Result<PublicArticleDetailVO> getArticle(@PathVariable Long id) {
        return Result.success(publicArticleService.getArticle(id));
    }
}
