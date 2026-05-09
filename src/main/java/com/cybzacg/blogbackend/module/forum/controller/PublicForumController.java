package com.cybzacg.blogbackend.module.forum.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.forum.model.publics.*;
import com.cybzacg.blogbackend.module.forum.service.PublicForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公开论坛接口。
 */
@RestController
@RequestMapping("/api/forum")
@Tag(name = "公开论坛接口")
@RequiredArgsConstructor
@Validated
public class PublicForumController {
    private final PublicForumService publicForumService;

    @GetMapping("/sections")
    @Operation(summary = "查询公开论坛版块")
    public Result<List<ForumSectionVO>> listSections() {
        return Result.success(publicForumService.listSections());
    }

    @GetMapping("/posts")
    @Operation(summary = "分页查询公开论坛帖子")
    public Result<PageResult<PublicForumPostVO>> pagePosts(ForumPostPageQuery query) {
        return Result.success(publicForumService.pagePosts(query));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "查询公开论坛帖子详情")
    public Result<PublicForumPostDetailVO> getPost(@PathVariable @NotNull @Positive Long id) {
        return Result.success(publicForumService.getPost(id));
    }

    @GetMapping("/posts/{postId}/replies")
    @Operation(summary = "分页查询公开论坛回复")
    public Result<PageResult<PublicForumReplyVO>> pageReplies(@PathVariable @NotNull @Positive Long postId,
                                                              @RequestParam(defaultValue = "1") @NotNull @Min(1) Long current,
                                                              @RequestParam(defaultValue = "10") @NotNull @Min(1) Long size) {
        return Result.success(publicForumService.pageReplies(postId, current, size));
    }
}
