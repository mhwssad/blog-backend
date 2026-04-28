package com.cybzacg.blogbackend.module.chat.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.model.user.ChatChannelApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatChannelApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatChannelApplicationVO;
import com.cybzacg.blogbackend.module.chat.service.UserChatChannelApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户侧频道创建申请控制器。
 */
@RestController
@RequestMapping("/api/user/chat/channel-applications")
@Tag(name = "用户频道创建申请")
@RequiredArgsConstructor
public class UserChatChannelApplicationController {
    private final UserChatChannelApplicationService userChatChannelApplicationService;

    @PostMapping
    @Operation(summary = "提交频道创建申请")
    public Result<ChatChannelApplicationVO> submitApplication(@Valid @RequestBody ChatChannelApplicationSubmitRequest request) {
        return Result.success(userChatChannelApplicationService.submitApplication(request));
    }

    @GetMapping("/latest")
    @Operation(summary = "查询最近一次频道创建申请")
    public Result<ChatChannelApplicationVO> getLatestApplication() {
        return Result.success(userChatChannelApplicationService.getLatestApplication());
    }

    @GetMapping
    @Operation(summary = "分页查询我的频道创建申请")
    public Result<PageResult<ChatChannelApplicationVO>> pageMyApplications(ChatChannelApplicationPageQuery query) {
        return Result.success(userChatChannelApplicationService.pageMyApplications(query));
    }
}
