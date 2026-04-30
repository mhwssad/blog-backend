package com.cybzacg.blogbackend.module.chat.conversation.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatConversationVO;
import com.cybzacg.blogbackend.module.chat.conversation.service.ChatTopicChannelPublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公开主题频道接口。
 */
@RestController
@RequestMapping("/api/public/chat/channels")
@Tag(name = "公开主题频道")
@RequiredArgsConstructor
public class PublicTopicChannelController {
    private final ChatTopicChannelPublicService chatTopicChannelPublicService;

    @GetMapping
    @Operation(summary = "分页查询公开主题频道列表")
    public Result<PageResult<ChatConversationVO>> pageChannels(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String categoryCode) {
        return Result.success(chatTopicChannelPublicService.pageChannels(current, size, categoryCode));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "查询主题频道详情")
    public Result<ChatConversationVO> getChannel(@PathVariable Long conversationId) {
        return Result.success(chatTopicChannelPublicService.getChannel(conversationId));
    }
}
