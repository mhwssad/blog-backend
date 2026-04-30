package com.cybzacg.blogbackend.module.chat.message.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.*;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatGroupMemberOperateRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMemberVO;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatMuteMemberRequest;
import com.cybzacg.blogbackend.module.chat.member.model.user.ChatReadStateVO;
import com.cybzacg.blogbackend.module.chat.message.model.user.*;
import com.cybzacg.blogbackend.module.chat.message.service.UserChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户侧聊天控制器。
 *
 * <p>负责单聊与群聊的会话管理、消息收发、已读游标推进及群成员操作等用户侧接口。
 */
@RestController
@RequestMapping("/api/user/chat")
@Tag(name = "用户聊天")
@RequiredArgsConstructor
public class UserChatController {
    private final UserChatService userChatService;

    @GetMapping("/conversations")
    @Operation(summary = "分页查询我的会话")
    public Result<PageResult<ChatConversationVO>> pageConversations(ChatConversationPageQuery query) {
        return Result.success(userChatService.pageMyConversations(query));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "查询会话详情")
    public Result<ChatConversationVO> getConversation(@PathVariable Long conversationId) {
        return Result.success(userChatService.getMyConversation(conversationId));
    }

    @PostMapping("/single-conversations")
    @Operation(summary = "打开或创建单聊会话")
    public Result<ChatConversationVO> openSingleConversation(@Valid @RequestBody ChatOpenSingleConversationRequest request) {
        return Result.success(userChatService.openSingleConversation(request));
    }

    @PostMapping("/conversations/{conversationId}/join")
    @Operation(summary = "加入公开频道或公开群")
    public Result<ChatConversationVO> joinConversation(@PathVariable Long conversationId) {
        return Result.success(userChatService.joinConversation(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/leave")
    @Operation(summary = "离开频道或公开群")
    public Result<Void> leaveConversation(@PathVariable Long conversationId) {
        userChatService.leaveConversation(conversationId);
        return Result.success();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "分页查询会话消息")
    public Result<PageResult<ChatMessageVO>> pageMessages(@PathVariable Long conversationId,
                                                          ChatMessagePageQuery query) {
        return Result.success(userChatService.pageMyMessages(conversationId, query));
    }

    @PostMapping("/messages/text")
    @Operation(summary = "发送文本消息")
    public Result<ChatMessageVO> sendTextMessage(@Valid @RequestBody ChatSendTextRequest request) {
        return Result.success(userChatService.sendTextMessage(request));
    }

    @PostMapping("/messages/file")
    @Operation(summary = "发送文件消息")
    public Result<ChatMessageVO> sendFileMessage(@Valid @RequestBody ChatSendFileRequest request) {
        return Result.success(userChatService.sendFileMessage(request));
    }

    @PutMapping("/messages/{messageId}")
    @Operation(summary = "编辑消息")
    public Result<ChatMessageVO> editMessage(@PathVariable Long messageId,
                                             @Valid @RequestBody ChatEditMessageRequest request) {
        return Result.success(userChatService.editMessage(messageId, request));
    }

    @PostMapping("/messages/{messageId}/revoke")
    @Operation(summary = "撤回消息")
    public Result<Void> revokeMessage(@PathVariable Long messageId) {
        userChatService.revokeMessage(messageId);
        return Result.success();
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "删除我的消息视图")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        userChatService.deleteMessage(messageId);
        return Result.success();
    }

    @PostMapping("/conversations/{conversationId}/read")
    @Operation(summary = "推进会话已读游标")
    public Result<ChatReadStateVO> markRead(@PathVariable Long conversationId,
                                            @Valid @RequestBody ChatMarkReadRequest request) {
        return Result.success(userChatService.markRead(conversationId, request));
    }

    @PostMapping("/groups")
    @Operation(summary = "创建群聊")
    public Result<ChatConversationVO> createGroup(@Valid @RequestBody ChatCreateGroupRequest request) {
        return Result.success(userChatService.createGroup(request));
    }

    @GetMapping("/groups/{conversationId}")
    @Operation(summary = "查询群聊详情")
    public Result<ChatConversationVO> getGroupDetail(@PathVariable Long conversationId) {
        return Result.success(userChatService.getGroupDetail(conversationId));
    }

    @GetMapping("/groups/{conversationId}/members")
    @Operation(summary = "查询群成员")
    public Result<List<ChatMemberVO>> listGroupMembers(@PathVariable Long conversationId) {
        return Result.success(userChatService.listGroupMembers(conversationId));
    }

    @PostMapping("/groups/{conversationId}/members")
    @Operation(summary = "邀请群成员")
    public Result<List<ChatMemberVO>> inviteGroupMembers(@PathVariable Long conversationId,
                                                         @Valid @RequestBody ChatGroupMemberOperateRequest request) {
        return Result.success(userChatService.inviteGroupMembers(conversationId, request));
    }

    @PutMapping("/groups/{conversationId}/admins/{memberUserId}")
    @Operation(summary = "设置群管理员")
    public Result<List<ChatMemberVO>> appointGroupAdmin(@PathVariable Long conversationId,
                                                        @PathVariable Long memberUserId) {
        return Result.success(userChatService.appointGroupAdmin(conversationId, memberUserId));
    }

    @DeleteMapping("/groups/{conversationId}/admins/{memberUserId}")
    @Operation(summary = "取消群管理员")
    public Result<List<ChatMemberVO>> removeGroupAdmin(@PathVariable Long conversationId,
                                                       @PathVariable Long memberUserId) {
        return Result.success(userChatService.removeGroupAdmin(conversationId, memberUserId));
    }

    @PutMapping("/groups/{conversationId}/owner")
    @Operation(summary = "转让群主")
    public Result<ChatConversationVO> transferGroupOwner(@PathVariable Long conversationId,
                                                         @Valid @RequestBody ChatTransferGroupOwnerRequest request) {
        return Result.success(userChatService.transferGroupOwner(conversationId, request));
    }

    @PutMapping("/groups/{conversationId}/members/{memberUserId}/mute")
    @Operation(summary = "设置群成员禁言")
    public Result<List<ChatMemberVO>> muteGroupMember(@PathVariable Long conversationId,
                                                      @PathVariable Long memberUserId,
                                                      @Valid @RequestBody ChatMuteMemberRequest request) {
        return Result.success(userChatService.muteGroupMember(conversationId, memberUserId, request));
    }

    @PutMapping("/groups/{conversationId}/notice")
    @Operation(summary = "更新群公告")
    public Result<ChatConversationVO> updateGroupNotice(@PathVariable Long conversationId,
                                                        @Valid @RequestBody ChatGroupNoticeUpdateRequest request) {
        return Result.success(userChatService.updateGroupNotice(conversationId, request));
    }

    @DeleteMapping("/groups/{conversationId}/members/{memberUserId}")
    @Operation(summary = "移除群成员")
    public Result<Void> removeGroupMember(@PathVariable Long conversationId,
                                          @PathVariable Long memberUserId) {
        userChatService.removeGroupMember(conversationId, memberUserId);
        return Result.success();
    }

    @PostMapping("/groups/{conversationId}/leave")
    @Operation(summary = "退出群聊")
    public Result<Void> leaveGroup(@PathVariable Long conversationId) {
        userChatService.leaveGroup(conversationId);
        return Result.success();
    }

    @DeleteMapping("/groups/{conversationId}")
    @Operation(summary = "解散群聊")
    public Result<Void> dissolveGroup(@PathVariable Long conversationId) {
        userChatService.dissolveGroup(conversationId);
        return Result.success();
    }
}
