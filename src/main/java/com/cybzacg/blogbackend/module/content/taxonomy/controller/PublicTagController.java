package com.cybzacg.blogbackend.module.content.taxonomy.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.shared.service.PublicContentQueryService;
import com.cybzacg.blogbackend.module.content.taxonomy.model.publics.PublicTagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台标签控制器。
 *
 * <p>负责对外暴露前台标签相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/tags")
@Tag(name = "前台标签接口")
@RequiredArgsConstructor
public class PublicTagController {
    private final PublicContentQueryService publicContentQueryService;

    @GetMapping
    @Operation(summary = "查询文章标签")
    public Result<List<PublicTagVO>> listTags(@RequestParam(defaultValue = "article") String targetType) {
        return Result.success(publicContentQueryService.listTags(targetType));
    }
}
