package com.cybzacg.blogbackend.module.chat.governance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteCreateRequest;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMutePageQuery;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteRecordVO;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatMuteGovernanceService;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 禁言管理后台接口。
 */
@RestController
@RequestMapping("/api/sys/chats/mutes")
@Tag(name = "后台禁言管理")
@RequiredArgsConstructor
public class ChatMuteAdminController {

    private final ChatMuteGovernanceService chatMuteGovernanceService;

    @PostMapping
    @Operation(summary = "创建禁言")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<ChatMuteRecordVO> createMute(@Valid @RequestBody ChatMuteCreateRequest request) {
        Long operatorId = SecurityUtils.requireUserId();
        return Result.success(chatMuteGovernanceService.createMute(request, operatorId));
    }

    @GetMapping
    @Operation(summary = "分页查询禁言记录")
    @PreAuthorize("@permission.hasPermission('content:chat:query')")
    public Result<PageResult<ChatMuteRecordVO>> pageMutes(ChatMutePageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 10L, 100L);
        Page<ChatMuteRecordVO> page = chatMuteGovernanceService.pageMutes(
                query.getUserId(), query.getScope(), query.getStatus(), current, size);
        return Result.success(PageResult.of(page, page.getRecords()));
    }

    @PutMapping("/{id}/release")
    @Operation(summary = "解除禁言")
    @PreAuthorize("@permission.hasPermission('content:chat:update')")
    public Result<Void> releaseMute(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        chatMuteGovernanceService.releaseMute(id, operatorId);
        return Result.success();
    }
}
