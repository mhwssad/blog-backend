package com.cybzacg.blogbackend.module.chat.governance.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.*;
import com.cybzacg.blogbackend.module.chat.member.model.admin.*;
import com.cybzacg.blogbackend.module.chat.message.model.admin.*;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天后台管理控制器。
 *
 * <p>负责会话查询、消息审计、成员管理与状态维护等后台运营接口。
 */
@RestController
@RequestMapping("/api/sys/chats")
@Tag(name = "后台聊天管理")
@RequiredArgsConstructor
public class ChatAdminController {
    private final ChatAdminService chatAdminService;

    @GetMapping("/conversations")
    @Operation(summary = "分页查询会话")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<PageResult<ChatAdminConversationVO>> pageConversations(ChatAdminConversationPageQuery query) {
        return Result.success(chatAdminService.pageConversations(query));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "查询会话详情")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<ChatAdminConversationVO> getConversation(@PathVariable Long conversationId) {
        return Result.success(chatAdminService.getConversation(conversationId));
    }

    @GetMapping("/conversations/{conversationId}/members")
    @Operation(summary = "查询会话成员")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<List<ChatMemberVO>> listMembers(@PathVariable Long conversationId) {
        return Result.success(chatAdminService.listMembers(conversationId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "分页查询会话消息")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<PageResult<ChatAdminMessageVO>> pageMessages(@PathVariable Long conversationId,
                                                               ChatAdminMessagePageQuery query) {
        return Result.success(chatAdminService.pageMessages(conversationId, query));
    }

    @GetMapping("/conversations/{conversationId}/messages/{messageId}")
    @Operation(summary = "查询消息详情")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<ChatAdminMessageDetailVO> getMessageDetail(@PathVariable Long conversationId,
                                                             @PathVariable Long messageId) {
        return Result.success(chatAdminService.getMessageDetail(conversationId, messageId));
    }

    @GetMapping("/conversations/{conversationId}/messages/{messageId}/receipts")
    @Operation(summary = "分页查询消息回执")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<PageResult<ChatAdminMessageReceiptVO>> pageMessageReceipts(@PathVariable Long conversationId,
                                                                             @PathVariable Long messageId,
                                                                             ChatAdminMessageReceiptPageQuery query) {
        return Result.success(chatAdminService.pageMessageReceipts(conversationId, messageId, query));
    }

    @PutMapping("/conversations/{conversationId}/members/{memberUserId}/role")
    @Operation(summary = "更新成员角色")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<List<ChatMemberVO>> updateMemberRole(@PathVariable Long conversationId,
                                                       @PathVariable Long memberUserId,
                                                       @Valid @RequestBody ChatAdminMemberRoleUpdateRequest request) {
        return Result.success(chatAdminService.updateMemberRole(conversationId, memberUserId, request));
    }

    @PutMapping("/conversations/{conversationId}/members/{memberUserId}/status")
    @Operation(summary = "更新成员状态")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<List<ChatMemberVO>> updateMemberStatus(@PathVariable Long conversationId,
                                                         @PathVariable Long memberUserId,
                                                         @Valid @RequestBody ChatAdminMemberStatusUpdateRequest request) {
        return Result.success(chatAdminService.updateMemberStatus(conversationId, memberUserId, request));
    }

    @PutMapping("/conversations/{conversationId}/members/{memberUserId}/mute")
    @Operation(summary = "更新成员禁言")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<List<ChatMemberVO>> updateMemberMute(@PathVariable Long conversationId,
                                                       @PathVariable Long memberUserId,
                                                       @Valid @RequestBody ChatAdminMemberMuteUpdateRequest request) {
        return Result.success(chatAdminService.updateMemberMute(conversationId, memberUserId, request));
    }

    @PostMapping("/conversations/{conversationId}/messages/{messageId}/revoke")
    @Operation(summary = "后台撤回消息")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<Void> revokeMessage(@PathVariable Long conversationId,
                                      @PathVariable Long messageId) {
        chatAdminService.revokeMessage(conversationId, messageId);
        return Result.success();
    }

    @PutMapping("/conversations/{conversationId}/status")
    @Operation(summary = "更新会话状态")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<Void> updateConversationStatus(@PathVariable Long conversationId,
                                                 @Valid @RequestBody ChatConversationStatusUpdateRequest request) {
        chatAdminService.updateConversationStatus(conversationId, request.getStatus());
        return Result.success();
    }
}


