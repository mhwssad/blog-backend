package com.cybzacg.blogbackend.module.dashboard.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.dashboard.model.admin.*;
import com.cybzacg.blogbackend.module.dashboard.service.DashboardAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台数据看板控制器。
 */
@RestController
@RequestMapping("/api/sys/dashboard")
@Tag(name = "后台数据看板")
@RequiredArgsConstructor
public class DashboardAdminController {
    private final DashboardAdminService dashboardAdminService;

    @GetMapping("/overview")
    @Operation(summary = "核心概览")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardOverviewVO> getOverview(DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getOverview(query));
    }

    @GetMapping("/content")
    @Operation(summary = "内容统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardContentVO> getContent(DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getContent(query));
    }

    @GetMapping("/community")
    @Operation(summary = "社区统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardCommunityVO> getCommunity(DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getCommunity(query));
    }

    @GetMapping("/ai")
    @Operation(summary = "AI 调用统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardAiVO> getAi(DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getAi(query));
    }

    @GetMapping("/governance")
    @Operation(summary = "治理统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardGovernanceVO> getGovernance(DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getGovernance(query));
    }
}
