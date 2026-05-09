package com.cybzacg.blogbackend.module.chat.member.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatGroupJoinApplicationReviewRequest;
import com.cybzacg.blogbackend.module.chat.member.service.ChatGroupJoinApplicationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台群入群申请管理控制器。
 */
@RestController
@RequestMapping("/api/sys/chats/group-join-applications")
@Tag(name = "后台群入群申请")
@RequiredArgsConstructor
public class ChatGroupJoinApplicationAdminController {
    private final ChatGroupJoinApplicationAdminService chatGroupJoinApplicationAdminService;

    @GetMapping
    @Operation(summary = "分页查询群入群申请")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<PageResult<ChatGroupJoinApplicationAdminVO>> pageApplications(ChatGroupJoinApplicationAdminPageQuery query) {
        return Result.success(chatGroupJoinApplicationAdminService.pageApplications(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询群入群申请详情")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<ChatGroupJoinApplicationAdminVO> getApplication(@PathVariable Long id) {
        return Result.success(chatGroupJoinApplicationAdminService.getApplication(id));
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "审核群入群申请")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<Void> reviewApplication(@PathVariable Long id,
                                          @Valid @RequestBody ChatGroupJoinApplicationReviewRequest request) {
        chatGroupJoinApplicationAdminService.reviewApplication(id, request);
        return Result.success();
    }
}
