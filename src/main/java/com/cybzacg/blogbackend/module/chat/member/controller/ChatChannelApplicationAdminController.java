package com.cybzacg.blogbackend.module.chat.member.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatChannelApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatChannelApplicationAdminVO;
import com.cybzacg.blogbackend.module.chat.member.model.admin.ChatChannelApplicationReviewRequest;
import com.cybzacg.blogbackend.module.chat.member.service.ChatChannelApplicationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台频道创建申请管理控制器。
 */
@RestController
@RequestMapping("/api/sys/chats/channel-applications")
@Tag(name = "后台频道创建申请")
@RequiredArgsConstructor
public class ChatChannelApplicationAdminController {
    private final ChatChannelApplicationAdminService chatChannelApplicationAdminService;

    @GetMapping
    @Operation(summary = "分页查询频道创建申请")
    @PreAuthorize("@permission.hasPermission('content:channel-application:query')")
    public Result<PageResult<ChatChannelApplicationAdminVO>> pageApplications(ChatChannelApplicationAdminPageQuery query) {
        return Result.success(chatChannelApplicationAdminService.pageApplications(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询频道创建申请详情")
    @PreAuthorize("@permission.hasPermission('content:channel-application:query')")
    public Result<ChatChannelApplicationAdminVO> getApplication(@PathVariable Long id) {
        return Result.success(chatChannelApplicationAdminService.getApplication(id));
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "审核频道创建申请")
    @PreAuthorize("@permission.hasPermission('content:channel-application:review')")
    public Result<Void> reviewApplication(@PathVariable Long id,
                                          @Valid @RequestBody ChatChannelApplicationReviewRequest request) {
        chatChannelApplicationAdminService.reviewApplication(id, request);
        return Result.success();
    }
}
