package com.cybzacg.blogbackend.module.chat.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupInviteLinkCreateRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupInviteLinkPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupInviteLinkVO;
import com.cybzacg.blogbackend.module.chat.service.UserChatGroupInviteLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户侧群邀请链接控制器。
 */
@RestController
@RequestMapping("/api/user/chat")
@Tag(name = "用户群邀请链接")
@RequiredArgsConstructor
public class UserChatGroupInviteLinkController {
    private final UserChatGroupInviteLinkService userChatGroupInviteLinkService;

    @PostMapping("/groups/{conversationId}/invite-links")
    @Operation(summary = "创建群邀请链接")
    public Result<ChatGroupInviteLinkVO> createInviteLink(@PathVariable Long conversationId,
                                                          @Valid @RequestBody(required = false) ChatGroupInviteLinkCreateRequest request) {
        return Result.success(userChatGroupInviteLinkService.createInviteLink(conversationId, request));
    }

    @GetMapping("/groups/{conversationId}/invite-links")
    @Operation(summary = "分页查询群邀请链接")
    public Result<PageResult<ChatGroupInviteLinkVO>> pageInviteLinks(@PathVariable Long conversationId,
                                                                     ChatGroupInviteLinkPageQuery query) {
        return Result.success(userChatGroupInviteLinkService.pageInviteLinks(conversationId, query));
    }

    @PutMapping("/groups/{conversationId}/invite-links/{inviteLinkId}/disable")
    @Operation(summary = "停用群邀请链接")
    public Result<Void> disableInviteLink(@PathVariable Long conversationId,
                                          @PathVariable Long inviteLinkId) {
        userChatGroupInviteLinkService.disableInviteLink(conversationId, inviteLinkId);
        return Result.success();
    }

    @PostMapping("/group-invite-links/{inviteToken}/join")
    @Operation(summary = "通过邀请链接加入群聊")
    public Result<Void> joinByInviteToken(@PathVariable String inviteToken) {
        userChatGroupInviteLinkService.joinByInviteToken(inviteToken);
        return Result.success();
    }
}
