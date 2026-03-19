package com.cybzacg.blogbackend.module.content.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.service.PublicContentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台分类控制器。
 *
 * <p>负责对外暴露前台分类相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "前台分类接口")
@RequiredArgsConstructor
public class PublicCategoryController {
    private final PublicContentQueryService publicContentQueryService;

    @GetMapping("/tree")
    @Operation(summary = "查询文章分类树")
    public Result<List<PublicCategoryTreeVO>> listCategoryTree() {
        return Result.success(publicContentQueryService.listCategoryTree());
    }
}
