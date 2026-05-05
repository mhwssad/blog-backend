package com.cybzacg.blogbackend.module.content.friendlink.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.account.model.admin.StatusUpdateRequest;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkPageQuery;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkSaveRequest;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkVO;
import com.cybzacg.blogbackend.module.content.friendlink.service.FriendLinkAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys/friend-links")
@Tag(name = "后台友情链接管理")
@RequiredArgsConstructor
public class FriendLinkAdminController {
    private final FriendLinkAdminService friendLinkAdminService;

    @GetMapping
    @Operation(summary = "分页查询友情链接")
    @PreAuthorize("@permission.hasPermission('content:friend-link:query')")
    public Result<com.cybzacg.blogbackend.core.web.PageResult<FriendLinkVO>> page(FriendLinkPageQuery query) {
        return Result.success(friendLinkAdminService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询友情链接详情")
    @PreAuthorize("@permission.hasPermission('content:friend-link:query')")
    public Result<FriendLinkVO> getById(@PathVariable Long id) {
        return Result.success(friendLinkAdminService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增友情链接")
    @PreAuthorize("@permission.hasPermission('content:friend-link:create')")
    public Result<FriendLinkVO> create(@Valid @RequestBody FriendLinkSaveRequest request) {
        return Result.success(friendLinkAdminService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改友情链接")
    @PreAuthorize("@permission.hasPermission('content:friend-link:update')")
    public Result<FriendLinkVO> update(@PathVariable Long id,
                                       @Valid @RequestBody FriendLinkSaveRequest request) {
        return Result.success(friendLinkAdminService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启停友情链接")
    @PreAuthorize("@permission.hasPermission('content:friend-link:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        friendLinkAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除友情链接")
    @PreAuthorize("@permission.hasPermission('content:friend-link:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        friendLinkAdminService.delete(id);
        return Result.success();
    }
}
