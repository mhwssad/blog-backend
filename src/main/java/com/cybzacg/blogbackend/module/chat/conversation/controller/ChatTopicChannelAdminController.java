package com.cybzacg.blogbackend.module.chat.conversation.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatAdminConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.model.admin.ChatTopicChannelSaveRequest;
import com.cybzacg.blogbackend.module.chat.conversation.service.ChatTopicChannelAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台主题频道管理控制器。
 */
@RestController
@RequestMapping("/api/sys/chats/topic-channels")
@Tag(name = "后台主题频道管理")
@RequiredArgsConstructor
public class ChatTopicChannelAdminController {
    private final ChatTopicChannelAdminService chatTopicChannelAdminService;

    @PostMapping
    @Operation(summary = "创建主题频道")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<ChatAdminConversationVO> createTopicChannel(@Valid @RequestBody ChatTopicChannelSaveRequest request) {
        return Result.success(chatTopicChannelAdminService.createTopicChannel(request));
    }

    @PutMapping("/{conversationId}")
    @Operation(summary = "编辑主题频道")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<ChatAdminConversationVO> updateTopicChannel(@PathVariable Long conversationId,
                                                             @Valid @RequestBody ChatTopicChannelSaveRequest request) {
        return Result.success(chatTopicChannelAdminService.updateTopicChannel(conversationId, request));
    }
}
