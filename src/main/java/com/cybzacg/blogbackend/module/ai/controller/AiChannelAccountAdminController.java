package com.cybzacg.blogbackend.module.ai.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountSaveRequest;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelAccountVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiChannelStatusRequest;
import com.cybzacg.blogbackend.module.ai.service.AiChannelAccountAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 渠道账号池后台管理接口。
 */
@RestController
@RequestMapping("/api/sys/ai/channels/{channelId}/accounts")
@RequiredArgsConstructor
@Tag(name = "AI渠道账号池管理")
public class AiChannelAccountAdminController {

    private final AiChannelAccountAdminService aiChannelAccountAdminService;

    @GetMapping
    @PreAuthorize("@permission.hasPermission('ai:channel-account:query')")
    @Operation(summary = "分页查询渠道账号列表")
    public Result<PageResult<AiChannelAccountVO>> listAccounts(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(aiChannelAccountAdminService.listAccounts(channelId, current, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permission.hasPermission('ai:channel-account:query')")
    @Operation(summary = "查询渠道账号详情")
    public Result<AiChannelAccountVO> getAccount(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        return Result.success(aiChannelAccountAdminService.getAccount(channelId, id));
    }

    @PostMapping
    @PreAuthorize("@permission.hasPermission('ai:channel-account:create')")
    @Operation(summary = "创建渠道账号")
    public Result<AiChannelAccountVO> createAccount(
            @PathVariable Long channelId,
            @Valid @RequestBody AiChannelAccountSaveRequest request) {
        return Result.success(aiChannelAccountAdminService.createAccount(
                channelId, request, SecurityUtils.requireUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permission.hasPermission('ai:channel-account:update')")
    @Operation(summary = "更新渠道账号")
    public Result<AiChannelAccountVO> updateAccount(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody AiChannelAccountSaveRequest request) {
        return Result.success(aiChannelAccountAdminService.updateAccount(
                channelId, id, request, SecurityUtils.requireUserId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permission.hasPermission('ai:channel-account:update')")
    @Operation(summary = "更新渠道账号状态")
    public Result<Void> updateAccountStatus(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody AiChannelStatusRequest request) {
        aiChannelAccountAdminService.updateAccountStatus(
                channelId, id, request.getStatus(), SecurityUtils.requireUserId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permission.hasPermission('ai:channel-account:delete')")
    @Operation(summary = "删除渠道账号")
    public Result<Void> deleteAccount(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        aiChannelAccountAdminService.deleteAccount(
                channelId, id, SecurityUtils.requireUserId());
        return Result.success();
    }
}
