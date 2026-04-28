package com.cybzacg.blogbackend.module.chat.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupJoinApplicationPageQuery;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupJoinApplicationVO;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupJoinApplyRequest;
import com.cybzacg.blogbackend.module.chat.model.user.ChatGroupJoinReviewRequest;
import com.cybzacg.blogbackend.module.chat.service.UserChatGroupJoinApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户侧入群申请控制器。
 */
@RestController
@RequestMapping("/api/user/chat")
@Tag(name = "用户入群申请")
@RequiredArgsConstructor
public class UserChatGroupJoinApplicationController {
    private final UserChatGroupJoinApplicationService userChatGroupJoinApplicationService;

    @PostMapping("/groups/{conversationId}/join-applications")
    @Operation(summary = "提交入群申请")
    public Result<ChatGroupJoinApplicationVO> submitApplication(@PathVariable Long conversationId,
                                                                @Valid @RequestBody(required = false) ChatGroupJoinApplyRequest request) {
        return Result.success(userChatGroupJoinApplicationService.submitApplication(conversationId, request));
    }

    @GetMapping("/group-join-applications")
    @Operation(summary = "分页查询我的入群申请")
    public Result<PageResult<ChatGroupJoinApplicationVO>> pageMyApplications(ChatGroupJoinApplicationPageQuery query) {
        return Result.success(userChatGroupJoinApplicationService.pageMyApplications(query));
    }

    @GetMapping("/groups/{conversationId}/join-applications")
    @Operation(summary = "分页查询群入群申请")
    public Result<PageResult<ChatGroupJoinApplicationVO>> pageGroupApplications(@PathVariable Long conversationId,
                                                                                ChatGroupJoinApplicationPageQuery query) {
        return Result.success(userChatGroupJoinApplicationService.pageGroupApplications(conversationId, query));
    }

    @PutMapping("/groups/{conversationId}/join-applications/{applicationId}/review")
    @Operation(summary = "审核入群申请")
    public Result<Void> reviewApplication(@PathVariable Long conversationId,
                                          @PathVariable Long applicationId,
                                          @Valid @RequestBody ChatGroupJoinReviewRequest request) {
        userChatGroupJoinApplicationService.reviewApplication(conversationId, applicationId, request);
        return Result.success();
    }
}
