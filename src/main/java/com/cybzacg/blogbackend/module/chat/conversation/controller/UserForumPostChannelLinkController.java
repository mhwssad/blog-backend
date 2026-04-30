package com.cybzacg.blogbackend.module.chat.conversation.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostChannelLinkVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ForumPostShareRequest;
import com.cybzacg.blogbackend.module.chat.conversation.service.ForumPostChannelLinkService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛帖子频道关联用户接口。
 */
@RestController
@RequestMapping("/api/user/chat/forum-links")
@Tag(name = "用户-帖子频道挂接")
@RequiredArgsConstructor
public class UserForumPostChannelLinkController {
    private final ForumPostChannelLinkService forumPostChannelLinkService;

    @PostMapping
    @Operation(summary = "分享帖子到频道")
    public Result<ForumPostChannelLinkVO> sharePostToChannel(@Valid @RequestBody ForumPostShareRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(forumPostChannelLinkService.sharePostToChannel(userId, request.getForumPostId(), request.getConversationId()));
    }

    @GetMapping("/posts/{forumPostId}")
    @Operation(summary = "查询帖子关联的频道")
    public Result<ForumPostChannelLinkVO> getPostLinkedChannel(@PathVariable Long forumPostId) {
        return Result.success(forumPostChannelLinkService.getPostLinkedChannel(forumPostId));
    }

    @GetMapping("/channels/{conversationId}")
    @Operation(summary = "分页查询频道关联的帖子")
    public Result<PageResult<ForumPostChannelLinkVO>> pageChannelLinks(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        return Result.success(forumPostChannelLinkService.pageChannelLinks(conversationId, current, size));
    }

    @DeleteMapping("/posts/{forumPostId}")
    @Operation(summary = "取消帖子与频道的关联")
    public Result<Void> unlinkPost(@PathVariable Long forumPostId) {
        Long userId = SecurityUtils.requireUserId();
        forumPostChannelLinkService.unlinkPost(userId, forumPostId);
        return Result.success();
    }
}
