package com.cybzacg.blogbackend.module.forum.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.forum.model.user.*;
import com.cybzacg.blogbackend.module.forum.service.UserForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户侧论坛接口。
 */
@RestController
@RequestMapping("/api/user/forum")
@Tag(name = "用户论坛接口")
@RequiredArgsConstructor
public class UserForumController {
    private final UserForumService userForumService;

    @GetMapping("/posts")
    @Operation(summary = "分页查询我的论坛帖子")
    public Result<PageResult<UserForumPostVO>> pageMyPosts(UserForumPostPageQuery query) {
        return Result.success(userForumService.pageMyPosts(query));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "查询我的论坛帖子详情")
    public Result<UserForumPostDetailVO> getMyPost(@PathVariable Long id) {
        return Result.success(userForumService.getMyPost(id));
    }

    @PostMapping("/posts")
    @Operation(summary = "创建论坛帖子")
    public Result<UserForumPostDetailVO> createPost(@Valid @RequestBody ForumPostSaveRequest request) {
        return Result.success(userForumService.createPost(request));
    }

    @PutMapping("/posts/{id}")
    @Operation(summary = "编辑我的论坛帖子")
    public Result<UserForumPostDetailVO> updatePost(@PathVariable Long id,
                                                    @Valid @RequestBody ForumPostSaveRequest request) {
        return Result.success(userForumService.updatePost(id, request));
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "删除我的论坛帖子")
    public Result<Void> deletePost(@PathVariable Long id) {
        userForumService.deletePost(id);
        return Result.success();
    }

    @PostMapping("/posts/{postId}/replies")
    @Operation(summary = "发表论坛回复")
    public Result<Void> createReply(@PathVariable Long postId,
                                    @Valid @RequestBody ForumReplySaveRequest request) {
        userForumService.createReply(postId, request);
        return Result.success();
    }

    @PutMapping("/replies/{replyId}")
    @Operation(summary = "编辑我的论坛回复")
    public Result<Void> updateReply(@PathVariable Long replyId,
                                    @Valid @RequestBody ForumReplySaveRequest request) {
        userForumService.updateReply(replyId, request);
        return Result.success();
    }

    @DeleteMapping("/replies/{replyId}")
    @Operation(summary = "删除我的论坛回复")
    public Result<Void> deleteReply(@PathVariable Long replyId) {
        userForumService.deleteReply(replyId);
        return Result.success();
    }

    @PostMapping("/posts/{postId}/likes")
    @Operation(summary = "点赞论坛帖子")
    public Result<Void> likePost(@PathVariable Long postId) {
        userForumService.likePost(postId);
        return Result.success();
    }

    @DeleteMapping("/posts/{postId}/likes")
    @Operation(summary = "取消点赞论坛帖子")
    public Result<Void> unlikePost(@PathVariable Long postId) {
        userForumService.unlikePost(postId);
        return Result.success();
    }

    @PostMapping("/posts/{postId}/collections")
    @Operation(summary = "收藏论坛帖子")
    public Result<Void> collectPost(@PathVariable Long postId,
                                    @RequestBody(required = false) ForumPostCollectRequest request) {
        userForumService.collectPost(postId, request);
        return Result.success();
    }

    @DeleteMapping("/posts/{postId}/collections")
    @Operation(summary = "取消收藏论坛帖子")
    public Result<Void> uncollectPost(@PathVariable Long postId) {
        userForumService.uncollectPost(postId);
        return Result.success();
    }

    @PostMapping("/posts/{postId}/channel-share")
    @Operation(summary = "分享论坛帖子到频道")
    public Result<ForumPostChannelLinkVO> sharePostToChannel(@PathVariable Long postId,
                                                             @Valid @RequestBody ForumPostChannelShareRequest request) {
        return Result.success(userForumService.sharePostToChannel(postId, request.getConversationId()));
    }
}
