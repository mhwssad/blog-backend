package com.cybzacg.blogbackend.module.auth.audit.controller;

import com.cybzacg.blogbackend.common.annotation.DisableSysLog;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogAdminVO;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysAuditLogPageQuery;
import com.cybzacg.blogbackend.module.auth.account.service.SuperAdminVerifier;
import com.cybzacg.blogbackend.module.auth.audit.service.SysAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志后台管理控制器。
 *
 * <p>负责对外暴露审计日志分页查询与详情查询接口，仅超级管理员可访问。
 */
@RestController
@DisableSysLog
@RequestMapping("/api/sys/audit-logs")
@Tag(name = "审计日志管理")
@RequiredArgsConstructor
public class SysAuditLogAdminController {
    private final SysAuditLogService sysAuditLogService;
    private final SuperAdminVerifier superAdminVerifier;

    @GetMapping
    @Operation(summary = "分页查询审计日志")
    @PreAuthorize("@permission.hasPermission('sys:audit:query')")
    public Result<PageResult<SysAuditLogAdminVO>> pageLogs(SysAuditLogPageQuery query) {
        superAdminVerifier.requireSuperAdmin();
        return Result.success(sysAuditLogService.pageLogs(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询审计日志详情")
    @PreAuthorize("@permission.hasPermission('sys:audit:query')")
    public Result<SysAuditLogAdminVO> getLog(@PathVariable Long id) {
        superAdminVerifier.requireSuperAdmin();
        return Result.success(sysAuditLogService.getLog(id));
    }
}
