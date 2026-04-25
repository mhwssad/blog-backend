package com.cybzacg.blogbackend.module.auth.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticeAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticeSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知后台管理控制器。
 *
 * <p>负责对外暴露系统通知后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/notices")
@Tag(name = "通知后台管理")
@RequiredArgsConstructor
public class SysNoticeAdminController {
    private final SysNoticeAdminService sysNoticeAdminService;

    @GetMapping
    @Operation(summary = "分页查询通知")
    @PreAuthorize("@permission.hasPermission('sys:notice:query')")
    public Result<PageResult<SysNoticeAdminVO>> pageNotices(SysNoticePageQuery query) {
        return Result.success(sysNoticeAdminService.pageNotices(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询通知详情")
    @PreAuthorize("@permission.hasPermission('sys:notice:query')")
    public Result<SysNoticeAdminVO> getNotice(@PathVariable Long id) {
        return Result.success(sysNoticeAdminService.getNotice(id));
    }

    @PostMapping
    @Operation(summary = "新增通知")
    @PreAuthorize("@permission.hasPermission('sys:notice:create')")
    public Result<SysNoticeAdminVO> createNotice(@Valid @RequestBody SysNoticeSaveRequest request) {
        return Result.success(sysNoticeAdminService.createNotice(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改通知")
    @PreAuthorize("@permission.hasPermission('sys:notice:update')")
    public Result<SysNoticeAdminVO> updateNotice(@PathVariable Long id,
                                                 @Valid @RequestBody SysNoticeSaveRequest request) {
        return Result.success(sysNoticeAdminService.updateNotice(id, request));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布通知")
    @PreAuthorize("@permission.hasPermission('sys:notice:publish')")
    public Result<Void> publishNotice(@PathVariable Long id) {
        sysNoticeAdminService.publishNotice(id);
        return Result.success();
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "撤回通知")
    @PreAuthorize("@permission.hasPermission('sys:notice:revoke')")
    public Result<Void> revokeNotice(@PathVariable Long id) {
        sysNoticeAdminService.revokeNotice(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    @PreAuthorize("@permission.hasPermission('sys:notice:delete')")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        sysNoticeAdminService.deleteNotice(id);
        return Result.success();
    }
}
