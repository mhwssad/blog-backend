package com.cybzacg.blogbackend.module.content.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.model.user.CommentSaveRequest;
import com.cybzacg.blogbackend.module.content.service.UserCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户评论控制器。
 *
 * <p>负责对外暴露用户评论相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/user/comments")
@Tag(name = "用户评论行为")
@RequiredArgsConstructor
public class UserCommentController {
    private final UserCommentService userCommentService;

    @PostMapping("/{id}/likes")
    @Operation(summary = "点赞评论")
    public Result<Void> likeComment(@PathVariable Long id) {
        userCommentService.likeComment(id);
        return Result.success();
    }

    @DeleteMapping("/{id}/likes")
    @Operation(summary = "取消点赞评论")
    public Result<Void> unlikeComment(@PathVariable Long id) {
        userCommentService.unlikeComment(id);
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "发表评论")
    public Result<Void> createComment(@Valid @RequestBody CommentSaveRequest request) {
        userCommentService.createComment(request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除我的评论")
    public Result<Void> deleteComment(@PathVariable Long id) {
        userCommentService.deleteComment(id);
        return Result.success();
    }
}
