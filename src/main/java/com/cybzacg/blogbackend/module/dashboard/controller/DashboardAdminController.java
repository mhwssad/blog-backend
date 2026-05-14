package com.cybzacg.blogbackend.module.dashboard.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.dashboard.model.admin.*;
import com.cybzacg.blogbackend.module.dashboard.service.DashboardAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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
    public Result<DashboardOverviewVO> getOverview(@Valid DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getOverview(query));
    }

    @GetMapping("/content")
    @Operation(summary = "内容统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardContentVO> getContent(@Valid DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getContent(query));
    }

    @GetMapping("/community")
    @Operation(summary = "社区统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardCommunityVO> getCommunity(@Valid DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getCommunity(query));
    }

    @GetMapping("/ai")
    @Operation(summary = "AI 调用统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardAiVO> getAi(@Valid DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getAi(query));
    }

    @GetMapping("/governance")
    @Operation(summary = "治理统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public Result<DashboardGovernanceVO> getGovernance(@Valid DashboardRangeQuery query) {
        return Result.success(dashboardAdminService.getGovernance(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出运营看板统计")
    @PreAuthorize("@permission.hasPermission('sys:dashboard:query')")
    public ResponseEntity<byte[]> exportDashboard(@Valid DashboardRangeQuery query) {
        byte[] content = dashboardAdminService.exportDashboard(query);
        String fileName = "dashboard-" + LocalDate.now() + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(encodedFileName, StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
