package com.cybzacg.blogbackend.module.report.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminPageQuery;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.admin.ReportHandleRequest;
import com.cybzacg.blogbackend.module.report.model.admin.ReportRejectRequest;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import com.cybzacg.blogbackend.module.report.service.ReportAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台举报管理控制器。
 */
@RestController
@RequestMapping("/api/sys/reports")
@Tag(name = "后台举报管理")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportAdminService reportAdminService;
    private final HttpServletRequest request;

    @GetMapping
    @Operation(summary = "分页筛选举报")
    @PreAuthorize("@permission.hasPermission('sys:report:query')")
    public Result<PageResult<ReportAdminVO>> pageReports(ReportAdminPageQuery query) {
        return Result.success(reportAdminService.pageReports(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "举报详情")
    @PreAuthorize("@permission.hasPermission('sys:report:query')")
    public Result<ReportAdminVO> getReportDetail(@PathVariable Long id) {
        return Result.success(reportAdminService.getReportDetail(id));
    }

    @PutMapping("/{id}/take")
    @Operation(summary = "接手举报")
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> claimReport(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        reportAdminService.claimReport(id, operatorId);
        return Result.success();
    }

    @PutMapping("/{id}/handle")
    @Operation(summary = "处理举报")
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> handleReport(@PathVariable Long id,
                                     @Valid @RequestBody ReportHandleRequest handleRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        reportAdminService.handleReport(id, operatorId, handleRequest, ip, ua);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "驳回举报")
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> rejectReport(@PathVariable Long id,
                                     @RequestBody ReportRejectRequest rejectRequest) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        reportAdminService.rejectReport(id, operatorId, rejectRequest.getRemark(), ip, ua);
        return Result.success();
    }

    @PutMapping("/{id}/override")
    @Operation(summary = "超管接管举报")
    @PreAuthorize("@permission.hasPermission('sys:report:handle')")
    public Result<Void> overrideClaim(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireUserId();
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        reportAdminService.overrideClaim(id, operatorId, ip, ua);
        return Result.success();
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "处理日志")
    @PreAuthorize("@permission.hasPermission('sys:report:query')")
    public Result<List<ReportHandleLogVO>> listHandleLogs(@PathVariable Long id) {
        return Result.success(reportAdminService.listHandleLogs(id));
    }
}
