package com.cybzacg.blogbackend.module.chat.member.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupSearchQuery;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatGroupSearchVO;
import com.cybzacg.blogbackend.module.chat.member.service.UserChatGroupDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧群聊发现控制器。
 */
@RestController
@RequestMapping("/api/user/chat/groups")
@Tag(name = "用户群聊发现")
@RequiredArgsConstructor
public class UserChatGroupDiscoveryController {
    private final UserChatGroupDiscoveryService userChatGroupDiscoveryService;

    @GetMapping("/search")
    @Operation(summary = "搜索公开群聊")
    public Result<PageResult<ChatGroupSearchVO>> searchGroups(ChatGroupSearchQuery query) {
        return Result.success(userChatGroupDiscoveryService.searchGroups(query));
    }
}
