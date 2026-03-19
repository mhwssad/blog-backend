package com.cybzacg.blogbackend.module.content.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentQuery;
import com.cybzacg.blogbackend.module.content.model.publics.PublicCommentVO;
import com.cybzacg.blogbackend.module.content.service.PublicContentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台评论控制器。
 *
 * <p>负责对外暴露前台评论相关接口，并作为请求进入业务层的统一入口。
 */
@Validated
@RestController
@RequestMapping("/api/comments")
@Tag(name = "前台评论接口")
@RequiredArgsConstructor
public class PublicCommentController {
    private final PublicContentQueryService publicContentQueryService;

    @GetMapping
    @Operation(summary = "查询评论树")
    public Result<List<PublicCommentVO>> listComments(@Valid PublicCommentQuery query) {
        return Result.success(publicContentQueryService.listComments(query));
    }
}
