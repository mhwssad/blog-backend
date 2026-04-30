package com.cybzacg.blogbackend.module.chat.conversation.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.conversation.model.user.ChatLobbyMessageVO;
import com.cybzacg.blogbackend.module.chat.message.service.UserChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大厅频道公开接口（访客可访问）。
 */
@RestController
@RequestMapping("/api/public/chat/lobby")
@Tag(name = "公开-大厅频道")
@RequiredArgsConstructor
public class PublicChatLobbyController {
    private final UserChatService userChatService;

    @GetMapping("/messages")
    @Operation(summary = "访客查看大厅消息")
    public Result<PageResult<ChatLobbyMessageVO>> pageLobbyMessages(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) Long beforeMessageId) {
        return Result.success(userChatService.pageLobbyMessages(current, size, beforeMessageId));
    }
}
