package com.cybzacg.blogbackend.module.chat.conversation.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatLobbySettingsUpdateRequest;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatLobbyPinnedMessageVO;
import com.cybzacg.blogbackend.module.chat.conversation.service.ChatLobbyAdminService;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatAdminMemberMuteUpdateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 大厅频道后台管理控制器。
 *
 * <p>负责大厅频道设置、消息置顶管理以及成员禁言/踢出等运营接口。
 */
@RestController
@RequestMapping("/api/sys/chats/lobby")
@Tag(name = "后台大厅频道管理")
@RequiredArgsConstructor
public class ChatLobbyAdminController {

    private final ChatLobbyAdminService chatLobbyAdminService;

    @PutMapping("/settings")
    @Operation(summary = "更新大厅频道设置")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<ChatConversationVO> updateLobbySettings(@Valid @RequestBody ChatLobbySettingsUpdateRequest request) {
        return Result.success(chatLobbyAdminService.updateLobbySettings(request));
    }

    @PostMapping("/messages/{messageId}/pin")
    @Operation(summary = "置顶大厅消息")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<Void> pinMessage(@PathVariable Long messageId) {
        chatLobbyAdminService.pinMessage(messageId);
        return Result.success();
    }

    @DeleteMapping("/messages/{messageId}/pin")
    @Operation(summary = "取消置顶大厅消息")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<Void> unpinMessage(@PathVariable Long messageId) {
        chatLobbyAdminService.unpinMessage(messageId);
        return Result.success();
    }

    @GetMapping("/messages/pinned")
    @Operation(summary = "分页查询大厅置顶消息")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<PageResult<ChatLobbyPinnedMessageVO>> pagePinnedMessages(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        return Result.success(chatLobbyAdminService.pagePinnedMessages(current, size));
    }

    @PutMapping("/members/{memberUserId}/mute")
    @Operation(summary = "禁言大厅用户")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<List<ChatMemberVO>> muteLobbyMember(@PathVariable Long memberUserId,
                                                      @Valid @RequestBody ChatAdminMemberMuteUpdateRequest request) {
        return Result.success(chatLobbyAdminService.muteLobbyMember(memberUserId, request));
    }

    @PutMapping("/members/{memberUserId}/kick")
    @Operation(summary = "踢出大厅用户")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<List<ChatMemberVO>> kickLobbyMember(@PathVariable Long memberUserId) {
        return Result.success(chatLobbyAdminService.kickLobbyMember(memberUserId));
    }
}
