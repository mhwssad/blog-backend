package com.cybzacg.blogbackend.module.auth.audit.controller;

import com.cybzacg.blogbackend.common.annotation.DisableSysLog;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.audit.service.SysLogAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统日志后台管理控制器。
 *
 * <p>负责对外暴露系统日志后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@DisableSysLog
@RequestMapping("/api/sys/logs")
@Tag(name = "系统日志管理")
@RequiredArgsConstructor
public class SysLogAdminController {
    private final SysLogAdminService sysLogAdminService;

    @GetMapping
    @Operation(summary = "分页查询日志")
    @PreAuthorize("@permission.hasPermission('sys:log:query')")
    public Result<PageResult<SysLogAdminVO>> pageLogs(SysLogPageQuery query) {
        return Result.success(sysLogAdminService.pageLogs(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询日志详情")
    @PreAuthorize("@permission.hasPermission('sys:log:query')")
    public Result<SysLogAdminVO> getLog(@PathVariable Long id) {
        return Result.success(sysLogAdminService.getLog(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除日志")
    @PreAuthorize("@permission.hasPermission('sys:log:delete')")
    public Result<Void> deleteLog(@PathVariable Long id) {
        sysLogAdminService.deleteLog(id);
        return Result.success();
    }

    @PostMapping("/clean")
    @Operation(summary = "按条件清理日志")
    @PreAuthorize("@permission.hasPermission('sys:log:clean')")
    public Result<Long> cleanLogs(@Valid @RequestBody SysLogCleanRequest request) {
        return Result.success(sysLogAdminService.cleanLogs(request));
    }
}
